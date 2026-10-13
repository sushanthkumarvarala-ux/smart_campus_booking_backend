package com.campus.booking.service.impl;

import com.campus.booking.dto.admin.DashboardStatsResponse;
import com.campus.booking.dto.booking.BookingRequest;
import com.campus.booking.dto.booking.BookingResponse;
import com.campus.booking.entity.Booking;
import com.campus.booking.entity.Resource;
import com.campus.booking.entity.User;
import com.campus.booking.enums.BookingStatus;
import com.campus.booking.enums.Role;
import com.campus.booking.exception.BadRequestException;
import com.campus.booking.exception.ConflictException;
import com.campus.booking.exception.ResourceNotFoundException;
import com.campus.booking.exception.UnauthorizedException;
import com.campus.booking.repository.BookingRepository;
import com.campus.booking.repository.ResourceRepository;
import com.campus.booking.repository.UserRepository;
import com.campus.booking.security.UserPrincipal;
import com.campus.booking.service.BookingService;
import com.campus.booking.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public BookingResponse createBooking(BookingRequest request, UserPrincipal currentUser) {
        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required to submit booking");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        LocalDateTime start = request.getStartTime();
        LocalDateTime end = request.getEndTime();

        // 1. Time logic validations
        if (!start.isBefore(end)) {
            throw new BadRequestException("Booking start time must be before end time");
        }

        if (start.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Booking cannot be scheduled in the past");
        }

        if (!start.toLocalDate().equals(end.toLocalDate())) {
            throw new BadRequestException("Bookings cannot span multiple calendar days");
        }

        // 2. Resource existence & pessimistic locking for concurrency safety
        Resource resource = resourceRepository.findByIdWithLock(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", request.getResourceId()));

        if (!resource.isActive()) {
            throw new BadRequestException("Resource '" + resource.getName() + "' is inactive and unavailable for bookings");
        }

        // 3. Resource operating hours validation
        LocalTime requestedStartTime = start.toLocalTime();
        LocalTime requestedEndTime = end.toLocalTime();

        if (requestedStartTime.isBefore(resource.getOpeningTime()) || requestedEndTime.isAfter(resource.getClosingTime())) {
            throw new BadRequestException(String.format(
                    "Booking time (%s - %s) falls outside resource operating hours (%s - %s)",
                    requestedStartTime, requestedEndTime, resource.getOpeningTime(), resource.getClosingTime()));
        }

        // 4. Overlap conflict validation
        // Conflict rule: resource_id = ? AND status IN (PENDING, APPROVED) AND start_time < requestedEnd AND end_time > requestedStart
        List<BookingStatus> activeStatuses = List.of(BookingStatus.PENDING, BookingStatus.APPROVED);
        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                resource.getId(), activeStatuses, start, end);

        if (!conflicts.isEmpty()) {
            Booking conflictingBooking = conflicts.get(0);
            throw new ConflictException(String.format(
                    "Resource '%s' is unavailable. Conflict with existing booking (ID: %d, %s to %s)",
                    resource.getName(), conflictingBooking.getId(),
                    conflictingBooking.getStartTime(), conflictingBooking.getEndTime()));
        }

        // 5. Build and save booking
        LocalDateTime now = LocalDateTime.now();
        Booking booking = Booking.builder()
                .user(user)
                .resource(resource)
                .startTime(start)
                .endTime(end)
                .purpose(request.getPurpose().trim())
                .status(BookingStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking #{} created for user {} on resource {}", savedBooking.getId(), user.getEmail(), resource.getName());

        // 6. Notify user of successful booking submission
        notificationService.sendNotification(user, String.format(
                "Your booking request #%d for '%s' on %s has been submitted and is pending review.",
                savedBooking.getId(), resource.getName(), start.toLocalDate()));

        return BookingResponse.fromEntity(savedBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(Long userId, BookingStatus status) {
        List<Booking> bookings;
        if (status != null) {
            bookings = bookingRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
        } else {
            bookings = bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }

        return bookings.stream()
                .map(BookingResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id, UserPrincipal currentUser) {
        Booking booking = findBookingOrThrow(id);

        boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ADMIN;
        boolean isOwner = currentUser != null && booking.getUser().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new UnauthorizedException("You do not have permission to view this booking record");
        }

        return BookingResponse.fromEntity(booking);
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(Long id, String reason, UserPrincipal currentUser) {
        Booking booking = findBookingOrThrow(id);

        boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ADMIN;
        boolean isOwner = currentUser != null && booking.getUser().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new UnauthorizedException("Only the booking owner or an administrator can cancel this booking");
        }

        // Cancellation allowed only if status is PENDING or APPROVED
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.APPROVED) {
            throw new BadRequestException("Cannot cancel booking with current status: " + booking.getStatus());
        }

        // Cancellation must occur before scheduled start time
        if (!LocalDateTime.now().isBefore(booking.getStartTime())) {
            throw new BadRequestException("Bookings cannot be cancelled once start time has arrived or passed");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        if (reason != null && !reason.trim().isEmpty()) {
            booking.setAdminRemark("Cancelled: " + reason.trim());
        }

        Booking updated = bookingRepository.save(booking);
        log.info("Booking #{} cancelled by user {}", id, currentUser.getEmail());

        notificationService.sendNotification(booking.getUser(), String.format(
                "Your booking #%d for '%s' scheduled for %s has been cancelled.",
                booking.getId(), booking.getResource().getName(), booking.getStartTime()));

        return BookingResponse.fromEntity(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getAdminBookings(BookingStatus status) {
        List<Booking> bookings;
        if (status != null) {
            bookings = bookingRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            bookings = bookingRepository.findAllByOrderByCreatedAtDesc();
        }

        return bookings.stream()
                .map(BookingResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public BookingResponse approveBooking(Long id, String remark) {
        Booking booking = findBookingOrThrow(id);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Only PENDING bookings can be approved. Current status: " + booking.getStatus());
        }

        if (booking.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Cannot approve booking as its scheduled start time has already passed");
        }

        // Double check no conflicting APPROVED booking exists
        List<Booking> approvedConflicts = bookingRepository.findConflictingBookingsExcluding(
                booking.getResource().getId(),
                booking.getId(),
                List.of(BookingStatus.APPROVED),
                booking.getStartTime(),
                booking.getEndTime());

        if (!approvedConflicts.isEmpty()) {
            throw new ConflictException("Another overlapping booking was already approved for this time slot");
        }

        booking.setStatus(BookingStatus.APPROVED);
        if (remark != null && !remark.trim().isEmpty()) {
            booking.setAdminRemark(remark.trim());
        }

        Booking updated = bookingRepository.save(booking);
        log.info("Booking #{} approved by administrator", id);

        notificationService.sendNotification(booking.getUser(), String.format(
                "Great news! Your booking #%d for '%s' on %s has been APPROVED.%s",
                booking.getId(), booking.getResource().getName(), booking.getStartTime().toLocalDate(),
                (remark != null && !remark.isBlank() ? " Admin note: " + remark : "")));

        return BookingResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public BookingResponse rejectBooking(Long id, String remark) {
        Booking booking = findBookingOrThrow(id);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Only PENDING bookings can be rejected. Current status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.REJECTED);
        booking.setAdminRemark(remark != null && !remark.trim().isEmpty() ? remark.trim() : "Rejected by Administrator");

        Booking updated = bookingRepository.save(booking);
        log.info("Booking #{} rejected by administrator with remark: {}", id, remark);

        notificationService.sendNotification(booking.getUser(), String.format(
                "Your booking #%d for '%s' was REJECTED. Reason: %s",
                booking.getId(), booking.getResource().getName(),
                (remark != null && !remark.isBlank() ? remark : "Schedule conflict or maintenance")));

        return BookingResponse.fromEntity(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        return DashboardStatsResponse.builder()
                .totalBookings(bookingRepository.count())
                .pendingBookings(bookingRepository.countByStatus(BookingStatus.PENDING))
                .approvedBookings(bookingRepository.countByStatus(BookingStatus.APPROVED))
                .rejectedBookings(bookingRepository.countByStatus(BookingStatus.REJECTED))
                .cancelledBookings(bookingRepository.countByStatus(BookingStatus.CANCELLED))
                .completedBookings(bookingRepository.countByStatus(BookingStatus.COMPLETED))
                .totalResources(resourceRepository.count())
                .activeResources(resourceRepository.countByActiveTrue())
                .totalUsers(userRepository.count())
                .totalStudents(userRepository.countByRole(Role.STUDENT))
                .totalFaculty(userRepository.countByRole(Role.FACULTY))
                .build();
    }

    private Booking findBookingOrThrow(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", id));
    }
}

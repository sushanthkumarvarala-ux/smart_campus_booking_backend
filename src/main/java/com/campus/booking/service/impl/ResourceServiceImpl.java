package com.campus.booking.service.impl;

import com.campus.booking.dto.resource.*;
import com.campus.booking.entity.Booking;
import com.campus.booking.entity.Resource;
import com.campus.booking.enums.BookingStatus;
import com.campus.booking.enums.ResourceType;
import com.campus.booking.exception.BadRequestException;
import com.campus.booking.exception.ResourceNotFoundException;
import com.campus.booking.repository.BookingRepository;
import com.campus.booking.repository.ResourceRepository;
import com.campus.booking.service.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resourceRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ResourceResponse> getAllResources(String keyword, ResourceType type, Boolean active) {
        String trimmedKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        List<Resource> resources = resourceRepository.searchResources(trimmedKeyword, type, active);
        return resources.stream()
                .map(ResourceResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceResponse getResourceById(Long id) {
        Resource resource = findResourceOrThrow(id);
        return ResourceResponse.fromEntity(resource);
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceAvailabilityResponse getResourceAvailability(Long id, LocalDate date) {
        Resource resource = findResourceOrThrow(id);
        LocalDate targetDate = date != null ? date : LocalDate.now();

        LocalDateTime dayStart = targetDate.atStartOfDay();
        LocalDateTime dayEnd = targetDate.atTime(LocalTime.MAX);

        List<BookingStatus> activeStatuses = List.of(BookingStatus.PENDING, BookingStatus.APPROVED);
        List<Booking> dayBookings = bookingRepository.findBookingsForResourceOnDay(
                id, activeStatuses, dayStart, dayEnd);

        List<BookedSlotDto> bookedSlots = dayBookings.stream()
                .map(b -> BookedSlotDto.builder()
                        .bookingId(b.getId())
                        .startTime(b.getStartTime())
                        .endTime(b.getEndTime())
                        .status(b.getStatus())
                        .build())
                .toList();

        // Generate hourly slots within opening and closing times
        List<TimeSlotDto> slots = new ArrayList<>();
        LocalTime opening = resource.getOpeningTime();
        LocalTime closing = resource.getClosingTime();

        LocalTime current = opening;
        while (current.isBefore(closing)) {
            LocalTime next = current.plusHours(1);
            if (next.isAfter(closing)) {
                next = closing;
            }

            LocalDateTime slotStart = targetDate.atTime(current);
            LocalDateTime slotEnd = targetDate.atTime(next);

            // A slot is unavailable if any active booking overlaps with [slotStart, slotEnd]
            boolean isBooked = dayBookings.stream().anyMatch(b ->
                    b.getStartTime().isBefore(slotEnd) && b.getEndTime().isAfter(slotStart));

            slots.add(TimeSlotDto.builder()
                    .startTime(current)
                    .endTime(next)
                    .available(!isBooked && resource.isActive())
                    .build());

            current = next;
        }

        return ResourceAvailabilityResponse.builder()
                .resourceId(resource.getId())
                .resourceName(resource.getName())
                .resourceType(resource.getType())
                .date(targetDate)
                .openingTime(resource.getOpeningTime())
                .closingTime(resource.getClosingTime())
                .active(resource.isActive())
                .bookedSlots(bookedSlots)
                .slots(slots)
                .build();
    }

    @Override
    @Transactional
    public ResourceResponse createResource(ResourceRequest request) {
        validateTimeRange(request.getOpeningTime(), request.getClosingTime());

        Resource resource = Resource.builder()
                .name(request.getName().trim())
                .type(request.getType())
                .location(request.getLocation().trim())
                .capacity(request.getCapacity())
                .description(request.getDescription())
                .active(request.getActive() != null ? request.getActive() : true)
                .openingTime(request.getOpeningTime())
                .closingTime(request.getClosingTime())
                .createdAt(LocalDateTime.now())
                .build();

        Resource saved = resourceRepository.save(resource);
        log.info("Created new resource '{}' with ID {}", saved.getName(), saved.getId());
        return ResourceResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public ResourceResponse updateResource(Long id, ResourceRequest request) {
        Resource resource = findResourceOrThrow(id);
        validateTimeRange(request.getOpeningTime(), request.getClosingTime());

        resource.setName(request.getName().trim());
        resource.setType(request.getType());
        resource.setLocation(request.getLocation().trim());
        resource.setCapacity(request.getCapacity());
        resource.setDescription(request.getDescription());
        if (request.getActive() != null) {
            resource.setActive(request.getActive());
        }
        resource.setOpeningTime(request.getOpeningTime());
        resource.setClosingTime(request.getClosingTime());

        Resource updated = resourceRepository.save(resource);
        log.info("Updated resource with ID {}", updated.getId());
        return ResourceResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteResource(Long id) {
        Resource resource = findResourceOrThrow(id);

        long bookingCount = bookingRepository.countByResourceId(id);
        if (bookingCount > 0) {
            // Soft delete by marking inactive to preserve booking integrity
            resource.setActive(false);
            resourceRepository.save(resource);
            log.info("Soft-deleted resource ID {} (marked inactive due to existing bookings)", id);
        } else {
            resourceRepository.delete(resource);
            log.info("Permanently deleted resource ID {}", id);
        }
    }

    private Resource findResourceOrThrow(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", id));
    }

    private void validateTimeRange(LocalTime opening, LocalTime closing) {
        if (opening == null || closing == null) {
            throw new BadRequestException("Opening and closing times are required");
        }
        if (!opening.isBefore(closing)) {
            throw new BadRequestException("Resource opening time (" + opening + ") must be before closing time (" + closing + ")");
        }
    }
}

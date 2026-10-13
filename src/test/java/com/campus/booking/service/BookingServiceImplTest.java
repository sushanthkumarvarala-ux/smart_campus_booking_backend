package com.campus.booking.service;

import com.campus.booking.dto.booking.BookingRequest;
import com.campus.booking.dto.booking.BookingResponse;
import com.campus.booking.entity.Booking;
import com.campus.booking.entity.Resource;
import com.campus.booking.entity.User;
import com.campus.booking.enums.BookingStatus;
import com.campus.booking.enums.ResourceType;
import com.campus.booking.enums.Role;
import com.campus.booking.exception.BadRequestException;
import com.campus.booking.exception.ConflictException;
import com.campus.booking.exception.UnauthorizedException;
import com.campus.booking.repository.BookingRepository;
import com.campus.booking.repository.ResourceRepository;
import com.campus.booking.repository.UserRepository;
import com.campus.booking.security.UserPrincipal;
import com.campus.booking.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User user;
    private UserPrincipal userPrincipal;
    private Resource resource;
    private BookingRequest bookingRequest;
    private Booking booking;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("Alex Johnson")
                .email("alex@campus.edu")
                .role(Role.STUDENT)
                .enabled(true)
                .build();

        userPrincipal = UserPrincipal.create(user);

        resource = Resource.builder()
                .id(10L)
                .name("Lecture Hall 101")
                .type(ResourceType.CLASSROOM)
                .location("Block A")
                .capacity(50)
                .active(true)
                .openingTime(LocalTime.of(8, 0))
                .closingTime(LocalTime.of(20, 0))
                .build();

        LocalDate futureDate = LocalDate.now().plusDays(2);
        LocalDateTime start = futureDate.atTime(10, 0);
        LocalDateTime end = futureDate.atTime(12, 0);

        bookingRequest = BookingRequest.builder()
                .resourceId(10L)
                .startTime(start)
                .endTime(end)
                .purpose("Study group session")
                .build();

        booking = Booking.builder()
                .id(100L)
                .user(user)
                .resource(resource)
                .startTime(start)
                .endTime(end)
                .purpose("Study group session")
                .status(BookingStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("createBooking() - Successfully creates booking with PENDING status")
    void createBooking_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(resourceRepository.findByIdWithLock(10L)).thenReturn(Optional.of(resource));
        when(bookingRepository.findConflictingBookings(eq(10L), anyCollection(), any(), any()))
                .thenReturn(List.of());
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        BookingResponse response = bookingService.createBooking(bookingRequest, userPrincipal);

        assertNotNull(response);
        assertEquals(BookingStatus.PENDING, response.getStatus());
        assertEquals("Lecture Hall 101", response.getResourceName());
        verify(resourceRepository).findByIdWithLock(10L);
        verify(notificationService).sendNotification(eq(user), anyString());
    }

    @Test
    @DisplayName("createBooking() - Throws BadRequestException when start is after end")
    void createBooking_InvalidTime() {
        bookingRequest.setStartTime(bookingRequest.getEndTime().plusHours(1));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class, () -> bookingService.createBooking(bookingRequest, userPrincipal));
        verify(resourceRepository, never()).findByIdWithLock(any());
    }

    @Test
    @DisplayName("createBooking() - Throws BadRequestException when resource is inactive")
    void createBooking_InactiveResource() {
        resource.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(resourceRepository.findByIdWithLock(10L)).thenReturn(Optional.of(resource));

        assertThrows(BadRequestException.class, () -> bookingService.createBooking(bookingRequest, userPrincipal));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("createBooking() - Throws BadRequestException when booking outside operating hours")
    void createBooking_OutsideOperatingHours() {
        // Resource closes at 20:00, requested end is 21:00
        bookingRequest.setEndTime(bookingRequest.getStartTime().toLocalDate().atTime(21, 0));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(resourceRepository.findByIdWithLock(10L)).thenReturn(Optional.of(resource));

        assertThrows(BadRequestException.class, () -> bookingService.createBooking(bookingRequest, userPrincipal));
    }

    @Test
    @DisplayName("createBooking() - Throws ConflictException when overlapping booking exists")
    void createBooking_OverlapConflict() {
        Booking existing = Booking.builder()
                .id(99L)
                .resource(resource)
                .startTime(bookingRequest.getStartTime())
                .endTime(bookingRequest.getEndTime())
                .status(BookingStatus.APPROVED)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(resourceRepository.findByIdWithLock(10L)).thenReturn(Optional.of(resource));
        when(bookingRepository.findConflictingBookings(eq(10L), anyCollection(), any(), any()))
                .thenReturn(List.of(existing));

        assertThrows(ConflictException.class, () -> bookingService.createBooking(bookingRequest, userPrincipal));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("approveBooking() - Approves pending booking and notifies user")
    void approveBooking_Success() {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(bookingRepository.findConflictingBookingsExcluding(eq(10L), eq(100L), anyCollection(), any(), any()))
                .thenReturn(List.of());
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        BookingResponse response = bookingService.approveBooking(100L, "Approved for study session");

        assertNotNull(response);
        assertEquals(BookingStatus.APPROVED, booking.getStatus());
        assertEquals("Approved for study session", booking.getAdminRemark());
        verify(notificationService).sendNotification(eq(user), contains("APPROVED"));
    }

    @Test
    @DisplayName("rejectBooking() - Rejects pending booking with admin remark")
    void rejectBooking_Success() {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        BookingResponse response = bookingService.rejectBooking(100L, "Room reserved for maintenance");

        assertNotNull(response);
        assertEquals(BookingStatus.REJECTED, booking.getStatus());
        assertEquals("Room reserved for maintenance", booking.getAdminRemark());
        verify(notificationService).sendNotification(eq(user), contains("REJECTED"));
    }

    @Test
    @DisplayName("cancelBooking() - Successfully cancels before start time")
    void cancelBooking_Success() {
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        BookingResponse response = bookingService.cancelBooking(100L, "Change of plans", userPrincipal);

        assertNotNull(response);
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        verify(notificationService).sendNotification(eq(user), contains("cancelled"));
    }

    @Test
    @DisplayName("cancelBooking() - Throws UnauthorizedException when unauthorized user attempts cancel")
    void cancelBooking_Unauthorized() {
        User otherUser = User.builder().id(999L).email("intruder@campus.edu").role(Role.STUDENT).build();
        UserPrincipal otherPrincipal = UserPrincipal.create(otherUser);

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(booking));

        assertThrows(UnauthorizedException.class, () -> bookingService.cancelBooking(100L, "Cancel", otherPrincipal));
    }
}

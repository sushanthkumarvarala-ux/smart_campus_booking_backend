package com.campus.booking.service;

import com.campus.booking.dto.resource.ResourceAvailabilityResponse;
import com.campus.booking.dto.resource.ResourceRequest;
import com.campus.booking.dto.resource.ResourceResponse;
import com.campus.booking.entity.Booking;
import com.campus.booking.entity.Resource;
import com.campus.booking.enums.BookingStatus;
import com.campus.booking.enums.ResourceType;
import com.campus.booking.exception.BadRequestException;
import com.campus.booking.exception.ResourceNotFoundException;
import com.campus.booking.repository.BookingRepository;
import com.campus.booking.repository.ResourceRepository;
import com.campus.booking.service.impl.ResourceServiceImpl;
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
class ResourceServiceImplTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private ResourceServiceImpl resourceService;

    private Resource resource;
    private ResourceRequest resourceRequest;

    @BeforeEach
    void setUp() {
        resource = Resource.builder()
                .id(1L)
                .name("Seminar Hall A")
                .type(ResourceType.SEMINAR_HALL)
                .location("Block A")
                .capacity(100)
                .description("Large hall")
                .active(true)
                .openingTime(LocalTime.of(8, 0))
                .closingTime(LocalTime.of(12, 0))
                .createdAt(LocalDateTime.now())
                .build();

        resourceRequest = ResourceRequest.builder()
                .name("Seminar Hall A")
                .type(ResourceType.SEMINAR_HALL)
                .location("Block A")
                .capacity(100)
                .description("Large hall")
                .active(true)
                .openingTime(LocalTime.of(8, 0))
                .closingTime(LocalTime.of(12, 0))
                .build();
    }

    @Test
    @DisplayName("getAllResources() - Returns matching list of resources")
    void getAllResources_Success() {
        when(resourceRepository.searchResources("Hall", ResourceType.SEMINAR_HALL, true))
                .thenReturn(List.of(resource));

        List<ResourceResponse> responses = resourceService.getAllResources("Hall", ResourceType.SEMINAR_HALL, true);

        assertEquals(1, responses.size());
        assertEquals("Seminar Hall A", responses.get(0).getName());
    }

    @Test
    @DisplayName("getResourceById() - Throws ResourceNotFoundException when ID missing")
    void getResourceById_NotFound() {
        when(resourceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> resourceService.getResourceById(99L));
    }

    @Test
    @DisplayName("getResourceAvailability() - Generates correct slot availability on date")
    void getResourceAvailability_Success() {
        LocalDate date = LocalDate.of(2026, 10, 15);
        Booking booking = Booking.builder()
                .id(101L)
                .resource(resource)
                .startTime(date.atTime(9, 0))
                .endTime(date.atTime(10, 0))
                .status(BookingStatus.APPROVED)
                .build();

        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
        when(bookingRepository.findBookingsForResourceOnDay(eq(1L), anyCollection(), any(), any()))
                .thenReturn(List.of(booking));

        ResourceAvailabilityResponse availability = resourceService.getResourceAvailability(1L, date);

        assertNotNull(availability);
        assertEquals(1, availability.getBookedSlots().size());
        assertEquals(4, availability.getSlots().size()); // 8-9, 9-10, 10-11, 11-12

        // Slot 8-9 is available
        assertTrue(availability.getSlots().get(0).isAvailable());
        // Slot 9-10 is booked
        assertFalse(availability.getSlots().get(1).isAvailable());
        // Slot 10-11 is available
        assertTrue(availability.getSlots().get(2).isAvailable());
    }

    @Test
    @DisplayName("createResource() - Throws BadRequestException when opening >= closing")
    void createResource_InvalidTime() {
        resourceRequest.setOpeningTime(LocalTime.of(18, 0));
        resourceRequest.setClosingTime(LocalTime.of(9, 0));

        assertThrows(BadRequestException.class, () -> resourceService.createResource(resourceRequest));
        verify(resourceRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteResource() - Soft deletes when bookings exist")
    void deleteResource_SoftDelete() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
        when(bookingRepository.countByResourceId(1L)).thenReturn(5L);

        resourceService.deleteResource(1L);

        assertFalse(resource.isActive());
        verify(resourceRepository).save(resource);
        verify(resourceRepository, never()).delete(resource);
    }
}

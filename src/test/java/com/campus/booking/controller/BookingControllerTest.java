package com.campus.booking.controller;

import com.campus.booking.dto.admin.DashboardStatsResponse;
import com.campus.booking.dto.booking.AdminRemarkRequest;
import com.campus.booking.dto.booking.BookingRequest;
import com.campus.booking.dto.booking.BookingResponse;
import com.campus.booking.enums.BookingStatus;
import com.campus.booking.enums.ResourceType;
import com.campus.booking.enums.Role;
import com.campus.booking.security.CustomUserDetailsService;
import com.campus.booking.security.JwtAuthenticationEntryPoint;
import com.campus.booking.security.JwtTokenProvider;
import com.campus.booking.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({BookingController.class, AdminBookingController.class, AdminDashboardController.class})
@AutoConfigureMockMvc(addFilters = false)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("POST /api/bookings - Should create booking successfully")
    void createBooking_ShouldReturnCreated() throws Exception {
        LocalDateTime futureStart = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);
        LocalDateTime futureEnd = futureStart.plusHours(2);

        BookingRequest request = BookingRequest.builder()
                .resourceId(1L)
                .startTime(futureStart)
                .endTime(futureEnd)
                .purpose("Technical Seminar Presentation")
                .build();

        BookingResponse response = BookingResponse.builder()
                .id(10L)
                .userId(1L)
                .userName("Alex Johnson")
                .userEmail("alex@campus.edu")
                .userRole(Role.STUDENT)
                .resourceId(1L)
                .resourceName("Seminar Hall A")
                .resourceType(ResourceType.SEMINAR_HALL)
                .startTime(futureStart)
                .endTime(futureEnd)
                .purpose("Technical Seminar Presentation")
                .status(BookingStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(bookingService.createBooking(any(BookingRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/bookings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.resourceName").value("Seminar Hall A"));
    }

    @Test
    @DisplayName("PATCH /api/admin/bookings/{id}/approve - Admin approves booking")
    void approveBooking_ShouldReturnApproved() throws Exception {
        BookingResponse response = BookingResponse.builder()
                .id(10L)
                .status(BookingStatus.APPROVED)
                .adminRemark("Approved by Admin")
                .build();

        when(bookingService.approveBooking(eq(10L), any())).thenReturn(response);

        mockMvc.perform(patch("/api/admin/bookings/10/approve")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminRemarkRequest("Approved by Admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.adminRemark").value("Approved by Admin"));
    }

    @Test
    @DisplayName("PATCH /api/admin/bookings/{id}/reject - Admin rejects booking")
    void rejectBooking_ShouldReturnRejected() throws Exception {
        BookingResponse response = BookingResponse.builder()
                .id(10L)
                .status(BookingStatus.REJECTED)
                .adminRemark("Maintenance ongoing")
                .build();

        when(bookingService.rejectBooking(eq(10L), any())).thenReturn(response);

        mockMvc.perform(patch("/api/admin/bookings/10/reject")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminRemarkRequest("Maintenance ongoing"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.adminRemark").value("Maintenance ongoing"));
    }

    @Test
    @DisplayName("GET /api/admin/dashboard/stats - Returns dashboard metrics")
    void getDashboardStats_ShouldReturnStats() throws Exception {
        DashboardStatsResponse stats = DashboardStatsResponse.builder()
                .totalBookings(25)
                .pendingBookings(5)
                .approvedBookings(18)
                .rejectedBookings(2)
                .totalResources(6)
                .activeResources(6)
                .totalUsers(40)
                .build();

        when(bookingService.getDashboardStats()).thenReturn(stats);

        mockMvc.perform(get("/api/admin/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBookings").value(25))
                .andExpect(jsonPath("$.pendingBookings").value(5))
                .andExpect(jsonPath("$.approvedBookings").value(18));
    }
}

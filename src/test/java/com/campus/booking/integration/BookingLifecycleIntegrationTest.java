package com.campus.booking.integration;

import com.campus.booking.dto.auth.AuthResponse;
import com.campus.booking.dto.auth.LoginRequest;
import com.campus.booking.dto.booking.BookingRequest;
import com.campus.booking.dto.booking.BookingResponse;
import com.campus.booking.enums.BookingStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BookingLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("End-to-End: Submission -> Conflict Rejection -> Admin Approval -> Cancellation Verification")
    void testEndToEndBookingLifecycle() throws Exception {
        // 1. Authenticate as Student
        LoginRequest studentLogin = LoginRequest.builder()
                .email("student@campus.edu")
                .password("Student@123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(studentLogin)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse studentAuth = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), AuthResponse.class);
        String studentToken = studentAuth.getToken();
        assertNotNull(studentToken);

        // 2. Submit a new Booking Request for tomorrow
        LocalDate targetDay = LocalDate.now().plusDays(3);
        LocalDateTime start = targetDay.atTime(11, 0);
        LocalDateTime end = targetDay.atTime(13, 0);

        BookingRequest bookingReq = BookingRequest.builder()
                .resourceId(1L)
                .startTime(start)
                .endTime(end)
                .purpose("Robotics Club Autonomous Drone Demonstration")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        BookingResponse createdBooking = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), BookingResponse.class);
        Long bookingId = createdBooking.getId();
        assertNotNull(bookingId);

        // 3. Attempt to Submit an OVERLAPPING Booking for the Same Resource -> Expect 409 CONFLICT!
        BookingRequest overlappingReq = BookingRequest.builder()
                .resourceId(1L)
                .startTime(targetDay.atTime(12, 0)) // overlaps 11:00-13:00
                .endTime(targetDay.atTime(14, 0))
                .purpose("Conflicting booking attempt")
                .build();

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overlappingReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // 4. Authenticate as Admin
        LoginRequest adminLogin = LoginRequest.builder()
                .email("admin@campus.edu")
                .password("Admin@123")
                .build();

        MvcResult adminLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse adminAuth = objectMapper.readValue(
                adminLoginResult.getResponse().getContentAsString(), AuthResponse.class);
        String adminToken = adminAuth.getToken();

        // 5. Admin Approves the Booking
        mockMvc.perform(patch("/api/admin/bookings/" + bookingId + "/approve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remark\": \"Approved for student demonstration\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.adminRemark").value("Approved for student demonstration"));

        // 6. Student Cancels the Approved Booking Prior to Start Time
        mockMvc.perform(patch("/api/bookings/" + bookingId + "/cancel")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remark\": \"Rescheduling to next week\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}

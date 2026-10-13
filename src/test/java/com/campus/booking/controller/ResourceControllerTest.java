package com.campus.booking.controller;

import com.campus.booking.dto.resource.ResourceAvailabilityResponse;
import com.campus.booking.dto.resource.ResourceRequest;
import com.campus.booking.dto.resource.ResourceResponse;
import com.campus.booking.enums.ResourceType;
import com.campus.booking.security.CustomUserDetailsService;
import com.campus.booking.security.JwtAuthenticationEntryPoint;
import com.campus.booking.security.JwtTokenProvider;
import com.campus.booking.service.ResourceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({ResourceController.class, AdminResourceController.class})
@AutoConfigureMockMvc(addFilters = false)
class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ResourceService resourceService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("GET /api/resources - Should return list of resources")
    void getAllResources_ShouldReturnList() throws Exception {
        ResourceResponse res = ResourceResponse.builder()
                .id(1L)
                .name("Computer Lab")
                .type(ResourceType.LAB)
                .location("Room 101")
                .capacity(30)
                .active(true)
                .openingTime(LocalTime.of(9, 0))
                .closingTime(LocalTime.of(17, 0))
                .createdAt(LocalDateTime.now())
                .build();

        when(resourceService.getAllResources(any(), any(), eq(true))).thenReturn(List.of(res));

        mockMvc.perform(get("/api/resources?active=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Computer Lab"))
                .andExpect(jsonPath("$[0].type").value("LAB"));
    }

    @Test
    @DisplayName("GET /api/resources/{id}/availability - Should return availability payload")
    void getResourceAvailability_ShouldReturnData() throws Exception {
        ResourceAvailabilityResponse availability = ResourceAvailabilityResponse.builder()
                .resourceId(1L)
                .resourceName("Computer Lab")
                .date(LocalDate.of(2026, 10, 20))
                .openingTime(LocalTime.of(9, 0))
                .closingTime(LocalTime.of(17, 0))
                .active(true)
                .bookedSlots(List.of())
                .slots(List.of())
                .build();

        when(resourceService.getResourceAvailability(eq(1L), any())).thenReturn(availability);

        mockMvc.perform(get("/api/resources/1/availability?date=2026-10-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceId").value(1L))
                .andExpect(jsonPath("$.resourceName").value("Computer Lab"));
    }

    @Test
    @DisplayName("POST /api/admin/resources - Should create resource successfully")
    void createResource_ShouldReturnCreated() throws Exception {
        ResourceRequest request = ResourceRequest.builder()
                .name("Physics Lab")
                .type(ResourceType.LAB)
                .location("Science Block")
                .capacity(40)
                .openingTime(LocalTime.of(8, 0))
                .closingTime(LocalTime.of(18, 0))
                .active(true)
                .build();

        ResourceResponse res = ResourceResponse.builder()
                .id(2L)
                .name("Physics Lab")
                .type(ResourceType.LAB)
                .location("Science Block")
                .capacity(40)
                .active(true)
                .openingTime(LocalTime.of(8, 0))
                .closingTime(LocalTime.of(18, 0))
                .createdAt(LocalDateTime.now())
                .build();

        when(resourceService.createResource(any(ResourceRequest.class))).thenReturn(res);

        mockMvc.perform(post("/api/admin/resources")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.name").value("Physics Lab"));
    }
}

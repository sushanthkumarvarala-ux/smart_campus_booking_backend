package com.campus.booking.controller;

import com.campus.booking.dto.auth.AuthResponse;
import com.campus.booking.dto.auth.LoginRequest;
import com.campus.booking.dto.auth.RegisterRequest;
import com.campus.booking.dto.auth.UserResponse;
import com.campus.booking.enums.Role;
import com.campus.booking.security.JwtAuthenticationEntryPoint;
import com.campus.booking.security.JwtTokenProvider;
import com.campus.booking.security.UserPrincipal;
import com.campus.booking.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private com.campus.booking.security.CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("POST /api/auth/register - Should register student successfully")
    void register_ShouldReturnCreated() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Jane Doe")
                .email("jane@campus.edu")
                .password("Password@123")
                .role(Role.STUDENT)
                .build();

        AuthResponse response = AuthResponse.builder()
                .token("mock-jwt-token")
                .type("Bearer")
                .id(1L)
                .name("Jane Doe")
                .email("jane@campus.edu")
                .role(Role.STUDENT)
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.email").value("jane@campus.edu"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    @DisplayName("POST /api/auth/register - Should return 400 when invalid email")
    void register_ShouldReturnBadRequest_WhenInvalidEmail() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .name("Jane Doe")
                .email("invalid-email")
                .password("Password@123")
                .role(Role.STUDENT)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    @DisplayName("POST /api/auth/login - Should return token on successful login")
    void login_ShouldReturnAuthResponse() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("admin@campus.edu")
                .password("Admin@123")
                .build();

        AuthResponse response = AuthResponse.builder()
                .token("admin-jwt-token")
                .type("Bearer")
                .id(99L)
                .name("Admin")
                .email("admin@campus.edu")
                .role(Role.ADMIN)
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("admin-jwt-token"))
                .andExpect(jsonPath("$.email").value("admin@campus.edu"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Should return 401 when invalid credentials")
    void login_ShouldReturnUnauthorized_WhenBadCredentials() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("admin@campus.edu")
                .password("WrongPassword")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
}

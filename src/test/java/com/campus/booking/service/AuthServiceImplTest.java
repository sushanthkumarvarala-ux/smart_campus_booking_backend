package com.campus.booking.service;

import com.campus.booking.dto.auth.AuthResponse;
import com.campus.booking.dto.auth.LoginRequest;
import com.campus.booking.dto.auth.RegisterRequest;
import com.campus.booking.entity.User;
import com.campus.booking.enums.Role;
import com.campus.booking.exception.BadRequestException;
import com.campus.booking.exception.ConflictException;
import com.campus.booking.repository.UserRepository;
import com.campus.booking.security.JwtTokenProvider;
import com.campus.booking.security.UserPrincipal;
import com.campus.booking.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .name("Alice Smith")
                .email("alice@campus.edu")
                .password("Secret@123")
                .role(Role.STUDENT)
                .build();

        savedUser = User.builder()
                .id(10L)
                .name("Alice Smith")
                .email("alice@campus.edu")
                .password("encodedPassword")
                .role(Role.STUDENT)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("register() - Successfully registers student and generates token")
    void register_Success() {
        when(userRepository.existsByEmail("alice@campus.edu")).thenReturn(false);
        when(passwordEncoder.encode("Secret@123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(tokenProvider.generateToken(eq("alice@campus.edu"), eq(10L), eq(Role.STUDENT)))
                .thenReturn("mocked.jwt.token");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("mocked.jwt.token", response.getToken());
        assertEquals("alice@campus.edu", response.getEmail());
        assertEquals(Role.STUDENT, response.getRole());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register() - Throws ConflictException when email is taken")
    void register_EmailConflict() {
        when(userRepository.existsByEmail("alice@campus.edu")).thenReturn(true);

        assertThrows(ConflictException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("register() - Throws BadRequestException when self-registering as ADMIN")
    void register_AdminRoleBlocked() {
        registerRequest.setRole(Role.ADMIN);
        when(userRepository.existsByEmail("alice@campus.edu")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("login() - Authenticates valid user credentials")
    void login_Success() {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("alice@campus.edu")
                .password("Secret@123")
                .build();

        UserPrincipal principal = UserPrincipal.create(savedUser);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn("login.jwt.token");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("login.jwt.token", response.getToken());
        assertEquals(10L, response.getId());
        assertEquals("alice@campus.edu", response.getEmail());
    }
}

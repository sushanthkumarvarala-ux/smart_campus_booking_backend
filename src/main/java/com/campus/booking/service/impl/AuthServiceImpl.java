package com.campus.booking.service.impl;

import com.campus.booking.dto.auth.AuthResponse;
import com.campus.booking.dto.auth.LoginRequest;
import com.campus.booking.dto.auth.RegisterRequest;
import com.campus.booking.dto.auth.UserResponse;
import com.campus.booking.entity.User;
import com.campus.booking.enums.Role;
import com.campus.booking.exception.BadRequestException;
import com.campus.booking.exception.ConflictException;
import com.campus.booking.exception.ResourceNotFoundException;
import com.campus.booking.repository.UserRepository;
import com.campus.booking.security.JwtTokenProvider;
import com.campus.booking.security.UserPrincipal;
import com.campus.booking.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Email address is already in use: " + normalizedEmail);
        }

        Role assignedRole = request.getRole() != null ? request.getRole() : Role.STUDENT;

        // Prevent self-registration as ADMIN for security
        if (assignedRole == Role.ADMIN) {
            throw new BadRequestException("Self-registration as ADMIN is not permitted");
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(assignedRole)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new user with email: {} and role: {}", savedUser.getEmail(), savedUser.getRole());

        String token = tokenProvider.generateToken(savedUser.getEmail(), savedUser.getId(), savedUser.getRole());

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        String token = tokenProvider.generateToken(authentication);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .id(userPrincipal.getId())
                .name(userPrincipal.getName())
                .email(userPrincipal.getEmail())
                .role(userPrincipal.getRole())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UserPrincipal currentUser) {
        if (currentUser == null) {
            throw new BadRequestException("No authenticated user in context");
        }

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

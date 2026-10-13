package com.campus.booking.service;

import com.campus.booking.dto.auth.AuthResponse;
import com.campus.booking.dto.auth.LoginRequest;
import com.campus.booking.dto.auth.RegisterRequest;
import com.campus.booking.dto.auth.UserResponse;
import com.campus.booking.security.UserPrincipal;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse getCurrentUser(UserPrincipal currentUser);
}

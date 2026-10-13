package com.campus.booking.controller;

import com.campus.booking.dto.admin.DashboardStatsResponse;
import com.campus.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin - Dashboard", description = "Admin dashboard statistics and system health indicators")
public class AdminDashboardController {

    private final BookingService bookingService;

    @GetMapping("/stats")
    @Operation(summary = "Get aggregated campus booking and resource statistics")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        DashboardStatsResponse stats = bookingService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }
}

package com.campus.booking.controller;

import com.campus.booking.dto.booking.AdminRemarkRequest;
import com.campus.booking.dto.booking.BookingResponse;
import com.campus.booking.enums.BookingStatus;
import com.campus.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin - Bookings", description = "Admin-only endpoints for reviewing, approving, and rejecting booking requests")
public class AdminBookingController {

    private final BookingService bookingService;

    @GetMapping
    @Operation(summary = "Get bookings filtered by status (defaults to all or specific status e.g. PENDING)")
    public ResponseEntity<List<BookingResponse>> getAdminBookings(
            @RequestParam(required = false) BookingStatus status) {

        List<BookingResponse> bookings = bookingService.getAdminBookings(status);
        return ResponseEntity.ok(bookings);
    }

    @PatchMapping("/{id}/approve")
    @Operation(summary = "Approve a pending booking request")
    public ResponseEntity<BookingResponse> approveBooking(
            @PathVariable Long id,
            @RequestBody(required = false) AdminRemarkRequest request) {

        String remark = request != null ? request.getRemark() : null;
        BookingResponse response = bookingService.approveBooking(id, remark);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/reject")
    @Operation(summary = "Reject a pending booking request with explanation remark")
    public ResponseEntity<BookingResponse> rejectBooking(
            @PathVariable Long id,
            @RequestBody(required = false) AdminRemarkRequest request) {

        String remark = request != null ? request.getRemark() : null;
        BookingResponse response = bookingService.rejectBooking(id, remark);
        return ResponseEntity.ok(response);
    }
}

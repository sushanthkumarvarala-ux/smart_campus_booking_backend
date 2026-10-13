package com.campus.booking.controller;

import com.campus.booking.dto.booking.AdminRemarkRequest;
import com.campus.booking.dto.booking.BookingRequest;
import com.campus.booking.dto.booking.BookingResponse;
import com.campus.booking.enums.BookingStatus;
import com.campus.booking.security.UserPrincipal;
import com.campus.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Bookings", description = "Endpoints for user booking submission, tracking, and cancellation")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @Operation(summary = "Submit a new resource booking request")
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody BookingRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        BookingResponse response = bookingService.createBooking(request, currentUser);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/my")
    @Operation(summary = "Get list of current user's bookings with optional status filter")
    public ResponseEntity<List<BookingResponse>> getMyBookings(
            @RequestParam(required = false) BookingStatus status,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        List<BookingResponse> bookings = bookingService.getMyBookings(currentUser.getId(), status);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking details by ID (owner or admin only)")
    public ResponseEntity<BookingResponse> getBookingById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        BookingResponse response = bookingService.getBookingById(id, currentUser);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a pending or approved booking prior to start time")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable Long id,
            @RequestBody(required = false) AdminRemarkRequest remarkRequest,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        String reason = remarkRequest != null ? remarkRequest.getRemark() : null;
        BookingResponse response = bookingService.cancelBooking(id, reason, currentUser);
        return ResponseEntity.ok(response);
    }
}

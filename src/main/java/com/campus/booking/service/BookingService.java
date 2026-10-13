package com.campus.booking.service;

import com.campus.booking.dto.admin.DashboardStatsResponse;
import com.campus.booking.dto.booking.BookingRequest;
import com.campus.booking.dto.booking.BookingResponse;
import com.campus.booking.enums.BookingStatus;
import com.campus.booking.security.UserPrincipal;

import java.util.List;

public interface BookingService {

    BookingResponse createBooking(BookingRequest request, UserPrincipal currentUser);

    List<BookingResponse> getMyBookings(Long userId, BookingStatus status);

    BookingResponse getBookingById(Long id, UserPrincipal currentUser);

    BookingResponse cancelBooking(Long id, String reason, UserPrincipal currentUser);

    List<BookingResponse> getAdminBookings(BookingStatus status);

    BookingResponse approveBooking(Long id, String remark);

    BookingResponse rejectBooking(Long id, String remark);

    DashboardStatsResponse getDashboardStats();
}

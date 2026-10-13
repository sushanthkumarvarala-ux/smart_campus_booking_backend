package com.campus.booking.dto.admin;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsResponse {

    private long totalBookings;
    private long pendingBookings;
    private long approvedBookings;
    private long rejectedBookings;
    private long cancelledBookings;
    private long completedBookings;

    private long totalResources;
    private long activeResources;

    private long totalUsers;
    private long totalStudents;
    private long totalFaculty;
}

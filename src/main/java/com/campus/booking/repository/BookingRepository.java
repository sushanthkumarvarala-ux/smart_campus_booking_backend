package com.campus.booking.repository;

import com.campus.booking.entity.Booking;
import com.campus.booking.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Check overlapping bookings for a given resource.
     * Overlap rule: start_time < requestedEnd AND end_time > requestedStart
     */
    @Query("SELECT b FROM Booking b WHERE b.resource.id = :resourceId " +
           "AND b.status IN :statuses " +
           "AND b.startTime < :requestedEnd " +
           "AND b.endTime > :requestedStart")
    List<Booking> findConflictingBookings(
            @Param("resourceId") Long resourceId,
            @Param("statuses") Collection<BookingStatus> statuses,
            @Param("requestedStart") LocalDateTime requestedStart,
            @Param("requestedEnd") LocalDateTime requestedEnd);

    /**
     * Find conflicting bookings excluding a specific booking ID (used for updates)
     */
    @Query("SELECT b FROM Booking b WHERE b.resource.id = :resourceId " +
           "AND b.id != :excludeBookingId " +
           "AND b.status IN :statuses " +
           "AND b.startTime < :requestedEnd " +
           "AND b.endTime > :requestedStart")
    List<Booking> findConflictingBookingsExcluding(
            @Param("resourceId") Long resourceId,
            @Param("excludeBookingId") Long excludeBookingId,
            @Param("statuses") Collection<BookingStatus> statuses,
            @Param("requestedStart") LocalDateTime requestedStart,
            @Param("requestedEnd") LocalDateTime requestedEnd);

    /**
     * Fetch all bookings for a resource within a single day for availability checking.
     */
    @Query("SELECT b FROM Booking b WHERE b.resource.id = :resourceId " +
           "AND b.status IN :statuses " +
           "AND b.startTime >= :dayStart " +
           "AND b.endTime <= :dayEnd " +
           "ORDER BY b.startTime ASC")
    List<Booking> findBookingsForResourceOnDay(
            @Param("resourceId") Long resourceId,
            @Param("statuses") Collection<BookingStatus> statuses,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd);

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Booking> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, BookingStatus status);

    List<Booking> findByStatusOrderByCreatedAtDesc(BookingStatus status);

    List<Booking> findAllByOrderByCreatedAtDesc();

    long countByStatus(BookingStatus status);

    long countByResourceId(Long resourceId);
}

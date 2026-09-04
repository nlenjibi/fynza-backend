package com.aoms.aomsbackend.attendance.repository;

import com.aoms.aomsbackend.seating.entity.SeatBooking;
import com.aoms.aomsbackend.seating.entity.SeatBookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link SeatBooking} records.
 */
@Repository
public interface NoShowSeatBookingRepository extends JpaRepository<SeatBooking, UUID> {

    /**
     * Returns all bookings for a specific building, date, and status.
     *
     * <p>Used by the no-show release job to fetch only {@code CONFIRMED} bookings,
     * which naturally excludes already-released or cancelled bookings and makes the
     * job idempotent without an explicit filter.
     *
     * @param buildingId  the building to query
     * @param bookingDate the date of interest
     * @param status      the booking status to filter on (e.g. {@link SeatBookingStatus#CONFIRMED})
     * @return matching bookings, possibly empty
     */
    List<SeatBooking> findByBuildingIdAndBookingDateAndStatus(
            UUID buildingId, LocalDate bookingDate, SeatBookingStatus status);
}

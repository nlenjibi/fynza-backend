package com.aoms.aomsbackend.seating.repository;

import com.aoms.aomsbackend.seating.entity.SeatBooking;
import com.aoms.aomsbackend.seating.entity.SeatBookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SeatBookingRepository extends JpaRepository<SeatBooking, UUID> {

    boolean existsByUserIdAndBookingDateAndStatus(UUID userId, LocalDate bookingDate, SeatBookingStatus status);

    List<SeatBooking> findByUserIdOrderByBookingDateDesc(UUID userId);

    Optional<SeatBooking> findByIdAndUserId(UUID id, UUID userId);

    boolean existsBySeatIdAndBookingDateGreaterThanEqualAndStatus(
            UUID seatId, LocalDate from, SeatBookingStatus status);

    Optional<SeatBooking> findByIdAndBuildingId(UUID id, UUID buildingId);

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

    /**
     * Finds a placeholder booking created by a block reservation for a given seat and date.
     * Used by the team-member seat-claim flow to detect and replace block placeholders.
     */
    Optional<SeatBooking> findBySeatIdAndBookingDateAndStatusAndBlockReservationIdIsNotNull(
            UUID seatId, LocalDate bookingDate, SeatBookingStatus status);

    /**
     * Returns all bookings that belong to a given block reservation.
     * Used during block cancellation to determine which bookings to cancel vs. preserve.
     */
    List<SeatBooking> findByBlockReservationId(UUID blockReservationId);

    /**
     * Checks whether a CONFIRMED booking already exists for a seat on a date.
     * Used as a conflict guard before creating a new individual booking.
     */
    boolean existsBySeatIdAndBookingDateAndStatus(UUID seatId, LocalDate bookingDate, SeatBookingStatus status);
}

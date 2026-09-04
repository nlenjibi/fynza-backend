package com.aoms.aomsbackend.seating.repository;

import com.aoms.aomsbackend.seating.entity.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SeatRepository extends JpaRepository<Seat, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id = :id")
    Optional<Seat> findByIdWithLock(@Param("id") UUID id);
    List<Seat> findByZoneIdAndActiveTrueAndDeletedAtIsNull(UUID zoneId);

    Optional<Seat> findByIdAndZoneIdAndActiveTrueAndDeletedAtIsNull(UUID id, UUID zoneId);

    boolean existsByZoneIdAndSeatNumberAndActiveTrueAndDeletedAtIsNull(UUID zoneId, String seatNumber);

    List<Seat> findByFloorIdAndDeletedAtIsNull(UUID floorId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE Seat SET is_active = false, deleted_at = CURRENT_TIMESTAMP WHERE zone_id = :zoneId AND is_active = true AND deleted_at IS NULL", nativeQuery = true)
    void deactivateAllByZoneId(@Param("zoneId") UUID zoneId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE Seat SET is_active = false, deleted_at = CURRENT_TIMESTAMP WHERE floor_id = :floorId AND is_active = true AND deleted_at IS NULL", nativeQuery = true)
    void deactivateAllByFloorId(@Param("floorId") UUID floorId);

    /**
     * Selects up to {@code count} available seat IDs in a room for a given date,
     * using pessimistic row-level locking (FOR UPDATE SKIP LOCKED) to prevent
     * concurrent block reservations from selecting the same seats.
     *
     * <p>A seat is considered available if it has no CONFIRMED booking on that date.
     *
     * @param roomId      the room to search within
     * @param bookingDate the date to check availability for
     * @param count       the maximum number of seats to return
     * @return up to {@code count} available seat UUIDs, locked for the current transaction
     */
    /**
     * Returns seat IDs as strings to ensure JDBC type-compatibility across both PostgreSQL
     * (production) and H2 (tests). Callers convert using {@link java.util.UUID#fromString}.
     */
    @Query(value = """
        SELECT CAST(s.id AS VARCHAR) FROM Seat s
        WHERE s.room_id = CAST(:roomId AS UUID)
          AND s.deleted_at IS NULL
          AND NOT EXISTS (
              SELECT 1 FROM seat_booking sb
              WHERE sb.seat_id = s.id
                AND sb.booking_date = :bookingDate
                AND sb.status = 'CONFIRMED'
          )
        LIMIT :count
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<String> selectAvailableSeatsForUpdate(
            @Param("roomId") UUID roomId,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("count") int count);
}

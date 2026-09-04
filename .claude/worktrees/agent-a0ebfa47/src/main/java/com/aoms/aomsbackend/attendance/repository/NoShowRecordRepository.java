package com.aoms.aomsbackend.attendance.repository;

import com.aoms.aomsbackend.attendance.entity.NoShowRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for {@link NoShowRecord} entries.
 *
 * <p>A unique constraint on {@code no_show_record.seat_booking_id} is enforced at the
 * database level, making the combination of this check and the constraint the primary
 * idempotency mechanism for the no-show release job.
 */
@Repository
public interface NoShowRecordRepository extends JpaRepository<NoShowRecord, UUID> {

    /**
     * Returns {@code true} if a no-show record already exists for the given booking.
     *
     * <p>Used as an in-process idempotency check before attempting to create a new
     * {@link NoShowRecord}, complementing the database-level unique constraint.
     *
     * @param seatBookingId the booking to check
     * @return {@code true} if a record exists, {@code false} otherwise
     */
    boolean existsBySeatBookingId(UUID seatBookingId);
}

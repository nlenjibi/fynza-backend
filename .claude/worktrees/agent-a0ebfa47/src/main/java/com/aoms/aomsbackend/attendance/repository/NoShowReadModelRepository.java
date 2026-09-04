package com.aoms.aomsbackend.attendance.repository;

import com.aoms.aomsbackend.attendance.entity.NoShowReadModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

public interface NoShowReadModelRepository
        extends JpaRepository<NoShowReadModel, UUID>, NoShowReadModelRepositoryCustom {

    boolean existsByNoShowRecordId(UUID noShowRecordId);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO no_show_record_read_model
                (id, no_show_record_id, user_id, organisation_id,
                 booking_date, seat_reference, auto_released_at)
            SELECT
                gen_random_uuid(),
                nsr.id,
                nsr.user_id,
                nsr.building_id,
                nsr.no_show_date,
                'Floor ' || f.floor_number || ' / ' || r.room_name || ' / Seat ' || s.seat_number,
                sb.auto_released_at
            FROM no_show_record    nsr
            JOIN seat_booking sb ON sb.id = nsr.seat_booking_id
            JOIN seat         s  ON s.id  = sb.seat_id
            JOIN room         r  ON r.id  = s.room_id
            JOIN floor        f  ON f.id  = s.floor_id
            WHERE nsr.no_show_date = :syncDate
            ON CONFLICT (no_show_record_id) DO NOTHING
            """, nativeQuery = true)
    int syncFromNoShowRecord(@Param("syncDate") LocalDate syncDate);
}

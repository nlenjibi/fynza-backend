package com.aoms.aomsbackend.attendance.service.impl;

import com.aoms.aomsbackend.attendance.entity.NoShowRecord;
import com.aoms.aomsbackend.seating.entity.SeatBooking;
import com.aoms.aomsbackend.seating.entity.SeatBookingStatus;
import com.aoms.aomsbackend.attendance.repository.NoShowRecordRepository;
import com.aoms.aomsbackend.attendance.repository.NoShowSeatBookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Persists the release of a single no-show booking in its own transaction so that
 * a failure for one booking does not roll back releases already committed for others
 * in the same job run.
 */
@Component
@RequiredArgsConstructor
public class NoShowBookingReleaseProcessor {

    private final NoShowSeatBookingRepository seatBookingRepository;
    private final NoShowRecordRepository noShowRecordRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(SeatBooking booking, LocalDate date) {
        booking.setStatus(SeatBookingStatus.RELEASED);
        booking.setAutoReleasedAt(OffsetDateTime.now());
        seatBookingRepository.save(booking);

        NoShowRecord record = new NoShowRecord();
        record.setSeatBookingId(booking.getId());
        record.setUserId(booking.getUserId());
        record.setBuildingId(booking.getBuildingId());
        record.setNoShowDate(date);
        noShowRecordRepository.save(record);
    }
}

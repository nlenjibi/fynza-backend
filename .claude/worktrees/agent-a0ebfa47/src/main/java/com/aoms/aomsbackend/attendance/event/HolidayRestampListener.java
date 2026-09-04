package com.aoms.aomsbackend.attendance.event;

import com.aoms.aomsbackend.attendance.service.AttendancePass2Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Triggers a Pass 2 restamp after a past public holiday is deleted.
 * The {@code AFTER_COMMIT} phase guarantees the restamp only runs when the delete transaction
 * has committed successfully — if the transaction rolls back, this listener is never invoked.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HolidayRestampListener {

    private final AttendancePass2Service pass2Service;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onHolidayDeleted(HolidayDeletedEvent event) {
        log.info("Restamping attendance after holiday deletion: locationId={}, date={}",
                event.locationId(), event.date());
        pass2Service.overlay(event.locationId(), event.officeId(), event.date());
    }
}

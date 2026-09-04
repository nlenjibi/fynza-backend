package com.aoms.aomsbackend.seating.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class SeatBookedEventHandler {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(SeatBookedEvent event) {
        log.info("Seat booked: bookingId={}, userId={}, seatId={}, date={}",
                event.bookingId(), event.userId(), event.seatId(), event.bookingDate());
    }
}

package com.aoms.aomsbackend.seating.event;

import java.time.LocalDate;
import java.util.UUID;

public record SeatBookedEvent(UUID bookingId, UUID userId, UUID seatId, LocalDate bookingDate) {}

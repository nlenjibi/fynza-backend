package com.aoms.aomsbackend.attendance.dto;

import com.aoms.aomsbackend.attendance.entity.BlockReservationStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class BlockReservationResponse {

    UUID id;
    UUID managerId;
    UUID buildingId;
    UUID roomId;
    LocalDate reservationDate;
    int seatCount;
    String notes;
    BlockReservationStatus status;
    List<UUID> seatBookingIds;
    OffsetDateTime createdAt;
}

package com.aoms.aomsbackend.seating.dto.request;

import com.aoms.aomsbackend.seating.entity.SeatStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSeatStatusRequest {

    @NotNull
    private SeatStatus status;
}

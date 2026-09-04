package com.aoms.aomsbackend.attendance.dto;

import com.aoms.aomsbackend.attendance.entity.SeatVisibilityMode;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSeatVisibilityRequest {

    @NotNull(message = "seatVisibilityMode is required")
    private SeatVisibilityMode seatVisibilityMode;
}

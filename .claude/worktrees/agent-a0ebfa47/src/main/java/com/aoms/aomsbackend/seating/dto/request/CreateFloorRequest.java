package com.aoms.aomsbackend.seating.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateFloorRequest {

    @NotBlank
    private String name;

    @NotNull
    private Integer floorNumber;
}

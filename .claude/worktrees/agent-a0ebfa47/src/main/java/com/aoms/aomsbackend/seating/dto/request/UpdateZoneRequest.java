package com.aoms.aomsbackend.seating.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateZoneRequest {

    @NotBlank
    private String name;
}

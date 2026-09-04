package com.aoms.aomsbackend.attendance.dto;

import lombok.Builder;
import lombok.Value;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;

@Value
@Builder
@JsonDeserialize(builder = ArmsOooRequest.ArmsOooRequestBuilder.class)
public class ArmsOooRequest {
    String requestId;
    String userId;
    String startDate;  // ISO date string
    String endDate;    // ISO date string
    String locationId;
    String status;

    @JsonPOJOBuilder(withPrefix = "")
    public static class ArmsOooRequestBuilder {}
}
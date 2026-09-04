package com.aoms.aomsbackend.attendance.dto;

import lombok.Builder;
import lombok.Value;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;

@Value
@Builder
@JsonDeserialize(builder = ArmsRemoteRequest.ArmsRemoteRequestBuilder.class)
public class ArmsRemoteRequest {
    String requestId;
    String userId;
    String[] dates;    // Array of ISO date strings, not datetime
    String locationId;
    String status;

    @JsonPOJOBuilder(withPrefix = "")
    public static class ArmsRemoteRequestBuilder {}
}
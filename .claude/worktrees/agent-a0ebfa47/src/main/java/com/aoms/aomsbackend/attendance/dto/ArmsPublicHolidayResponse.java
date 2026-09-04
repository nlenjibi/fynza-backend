package com.aoms.aomsbackend.attendance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;

@Value
@Builder
@JsonDeserialize(builder = ArmsPublicHolidayResponse.ArmsPublicHolidayBuilder.class)
public class ArmsPublicHolidayResponse {
    String id;
    String title;
    String description;

    @JsonProperty("start_day")
    String startDay;    // ISO date string

    @JsonProperty("end_day")
    String endDay;      // ISO date string

    String country;

    @JsonProperty("is_archived")
    Boolean isArchived;

    @JsonPOJOBuilder(withPrefix = "")
    public static class ArmsPublicHolidayBuilder {}
}

package com.aoms.aomsbackend.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoShowReportRecordDto {

    private UUID noShowRecordId;
    private UUID employeeId;
    private String employeeName;
    private String department;
    private LocalDate bookingDate;
    private String seatReference;
    private Instant autoReleasedAt;
    private int noShowCountInPeriod;
}

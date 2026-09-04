package com.aoms.aomsbackend.attendance.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class WeekPeriodId implements Serializable {
    private UUID userId;
    private Integer year;
    private Integer weekNumber;
}

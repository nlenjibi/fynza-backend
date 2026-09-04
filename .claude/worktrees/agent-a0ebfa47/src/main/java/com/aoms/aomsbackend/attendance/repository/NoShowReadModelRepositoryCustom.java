package com.aoms.aomsbackend.attendance.repository;

import com.aoms.aomsbackend.attendance.dto.NoShowReportRecordDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface NoShowReadModelRepositoryCustom {

    Page<NoShowReportRecordDto> findReportPage(
            UUID organisationId,
            LocalDate fromDate,
            LocalDate toDate,
            UUID employeeId,
            String department,
            Pageable pageable
    );
}

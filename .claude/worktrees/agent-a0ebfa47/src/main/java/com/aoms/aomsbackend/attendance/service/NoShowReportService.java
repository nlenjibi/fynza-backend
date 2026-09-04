package com.aoms.aomsbackend.attendance.service;

import com.aoms.aomsbackend.attendance.dto.NoShowReportRecordDto;
import com.aoms.aomsbackend.common.responses.PaginatedResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.LocalDate;
import java.util.UUID;

public interface NoShowReportService {

    PaginatedResponse<NoShowReportRecordDto> getReport(
            HttpServletRequest request,
            LocalDate fromDate,
            LocalDate toDate,
            UUID employeeId,
            String department,
            UUID organisationId,
            int page,
            int size
    );

    StreamingResponseBody exportCsv(
            HttpServletRequest request,
            LocalDate fromDate,
            LocalDate toDate,
            UUID employeeId,
            String department,
            UUID organisationId
    );
}

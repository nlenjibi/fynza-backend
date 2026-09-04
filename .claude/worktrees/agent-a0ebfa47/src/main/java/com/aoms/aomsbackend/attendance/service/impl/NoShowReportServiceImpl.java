package com.aoms.aomsbackend.attendance.service.impl;

import com.aoms.aomsbackend.attendance.dto.NoShowReportRecordDto;
import com.aoms.aomsbackend.attendance.repository.NoShowReadModelRepository;
import com.aoms.aomsbackend.attendance.service.NoShowReportService;
import com.aoms.aomsbackend.auth.constant.SessionAttribute;
import com.aoms.aomsbackend.auth.entity.UserRole;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.auth.repository.UserRoleRepository;
import com.aoms.aomsbackend.common.exception.BadRequestException;
import com.aoms.aomsbackend.common.exception.ForbiddenException;
import com.aoms.aomsbackend.common.exception.SessionExpiredException;
import com.aoms.aomsbackend.common.responses.PaginatedResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoShowReportServiceImpl implements NoShowReportService {

    private static final int MAX_EXPORT_DAYS = 90;
    private static final String[] CSV_HEADERS = {
        "Employee Name", "Employee ID", "Department",
        "Booking Date", "Seat Reference", "Auto Released At", "No-Show Count (Period)"
    };

    private final NoShowReadModelRepository noShowRepo;
    private final UserRoleRepository userRoleRepository;

    @Override
    public PaginatedResponse<NoShowReportRecordDto> getReport(
            HttpServletRequest request,
            LocalDate fromDate,
            LocalDate toDate,
            UUID employeeId,
            String department,
            UUID organisationId,
            int page,
            int size) {

        SessionContext ctx = resolveSession(request);
        UUID resolvedOrgId = resolveOrganisationId(ctx, organisationId);

        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.clamp(size, 1, 100)
        );

        Page<NoShowReportRecordDto> result = noShowRepo.findReportPage(
                resolvedOrgId, fromDate, toDate, employeeId, department, pageable);

        return PaginatedResponse.from(result);
    }

    @Override
    public StreamingResponseBody exportCsv(
            HttpServletRequest request,
            LocalDate fromDate,
            LocalDate toDate,
            UUID employeeId,
            String department,
            UUID organisationId) {

        SessionContext ctx = resolveSession(request);
        validateExportWindow(fromDate, toDate);
        UUID resolvedOrgId = resolveOrganisationId(ctx, organisationId);

        return outputStream -> {
            PrintWriter writer = new PrintWriter(outputStream);
            writer.println(String.join(",", CSV_HEADERS));

            int page = 0;
            Page<NoShowReportRecordDto> batch;

            do {
                Pageable pageable = PageRequest.of(page, 100);
                batch = noShowRepo.findReportPage(
                        resolvedOrgId, fromDate, toDate, employeeId, department, pageable);
                batch.getContent().forEach(r -> writer.println(toCsvRow(r)));
                writer.flush();
                page++;
            } while (batch.hasNext());
        };
    }

    // ── session helpers ────────────────────────────────────────────────────────

    private SessionContext resolveSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new SessionExpiredException("Session is invalid or expired.");
        }

        String userId   = resolveUserId(session);
        List<String> roles = resolveRoles(session);

        boolean isFacilitiesAdmin = roles.contains("ROLE_FACILITIES_ADMIN");
        boolean isSuperAdmin      = roles.contains("ROLE_SUPER_ADMIN");

        if (!isFacilitiesAdmin && !isSuperAdmin) {
            throw new ForbiddenException();
        }

        return new SessionContext(UUID.fromString(userId), isSuperAdmin);
    }

    private String resolveUserId(HttpSession session) {
        String userId = (String) session.getAttribute(SessionAttribute.USER_ID.getKey());
        if (userId == null) {
            userId = (String) session.getAttribute(SessionAttribute.V2_USER_ID.getKey());
        }
        if (userId == null) {
            throw new SessionExpiredException("Session is invalid or expired.");
        }
        return userId;
    }

    @SuppressWarnings("unchecked")
    private List<String> resolveRoles(HttpSession session) {
        List<String> roles = (List<String>) session.getAttribute(SessionAttribute.ROLES.getKey());
        if (roles == null) {
            roles = (List<String>) session.getAttribute(SessionAttribute.V2_ROLES.getKey());
        }
        return roles != null ? roles : List.of();
    }

    // ── org scope helper ───────────────────────────────────────────────────────

    private UUID resolveOrganisationId(SessionContext ctx, UUID requestedOrgId) {
        if (ctx.isSuperAdmin()) {
            return requestedOrgId; // null = all organisations
        }
        return userRoleRepository
                .findByUserIdAndDeletedAtIsNull(ctx.userId()).stream()
                .filter(r -> r.getRole() == UserRoleType.FACILITIES_ADMIN)
                .map(UserRole::getOrganisationId)
                .findFirst()
                .orElse(null);
    }

    // ── export helpers ────────────────────────────────────────────────────────

    private void validateExportWindow(LocalDate fromDate, LocalDate toDate) {
        if (ChronoUnit.DAYS.between(fromDate, toDate) > MAX_EXPORT_DAYS) {
            throw new BadRequestException(
                    "Export window cannot exceed " + MAX_EXPORT_DAYS + " days.");
        }
    }

    private String toCsvRow(NoShowReportRecordDto r) {
        return String.join(",",
                escapeCsv(r.getEmployeeName()),
                escapeCsv(r.getEmployeeId() != null ? r.getEmployeeId().toString() : ""),
                escapeCsv(r.getDepartment()),
                r.getBookingDate() != null ? r.getBookingDate().toString() : "",
                escapeCsv(r.getSeatReference()),
                r.getAutoReleasedAt() != null
                        ? DateTimeFormatter.ISO_INSTANT.format(r.getAutoReleasedAt()) : "",
                String.valueOf(r.getNoShowCountInPeriod())
        );
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // ── inner record ──────────────────────────────────────────────────────────

    private record SessionContext(UUID userId, boolean isSuperAdmin) {}
}

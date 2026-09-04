package com.aoms.aomsbackend.audit.controller;

import com.aoms.aomsbackend.audit.dto.AuditLogFilter;
import com.aoms.aomsbackend.audit.dto.AuditLogResponse;
import com.aoms.aomsbackend.audit.service.AuditLogQueryService;
import com.aoms.aomsbackend.auth.constant.SessionAttribute;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.common.annotation.RequiresRole;
import com.aoms.aomsbackend.common.exception.SessionExpiredException;
import com.aoms.aomsbackend.common.responses.PaginatedResponse;
import com.aoms.aomsbackend.common.responses.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@RequiresRole(UserRoleType.FACILITIES_ADMIN)
@Tag(name = "Audit Logs", description = "Paginated audit log query — accessible to Facilities Admin, HR, and Super Admin")
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;

    @Operation(
            summary = "Query audit logs",
            description = "Returns a paginated list of audit log entries. MANAGER and below are rejected at the service layer.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Paginated audit log results"),
                    @ApiResponse(responseCode = "401", description = "Session invalid or expired"),
                    @ApiResponse(responseCode = "403", description = "Insufficient role")
            }
    )
    @GetMapping
    public ResponseEntity<ResponseWrapper<PaginatedResponse<AuditLogResponse>>> query(
            @Parameter(description = "Filter by actor user ID")
            @RequestParam(required = false) UUID actorId,

            @Parameter(description = "Filter by entity type, e.g. SeatBooking")
            @RequestParam(required = false) String entityType,

            @Parameter(description = "Filter by entity ID")
            @RequestParam(required = false) UUID entityId,

            @Parameter(description = "Filter by location ID")
            @RequestParam(required = false) UUID locationId,

            @Parameter(description = "Filter by action, e.g. SEAT_BOOKED")
            @RequestParam(required = false) String action,

            @Parameter(description = "Return entries on or after this timestamp (ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,

            @Parameter(description = "Return entries on or before this timestamp (ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,

            @Parameter(description = "Zero-based page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)")   @RequestParam(defaultValue = "20") int size,

            HttpServletRequest request) {

        UUID callerId   = resolveCallerId(request);
        UUID callerOrg  = resolveOrgId(request);

        AuditLogFilter filter = buildFilter(actorId, entityType, entityId, locationId, action, from, to);
        PageRequest pageable  = PageRequest.of(page, Math.min(size, 100), Sort.by("occurredAt").descending());

        Page<AuditLogResponse> result = auditLogQueryService.query(filter, pageable, callerId, callerOrg);
        return ResponseEntity.ok(ResponseWrapper.success("All orders retrieved successfully", PaginatedResponse.from(result)));

    }

    // ── session helpers ───────────────────────────────────────────────────────────

    private UUID resolveCallerId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) throw new SessionExpiredException("Session is invalid or expired.");
        String id = (String) session.getAttribute(SessionAttribute.USER_ID.getKey());
        if (id == null) id = (String) session.getAttribute(SessionAttribute.V2_USER_ID.getKey());
        if (id == null) throw new SessionExpiredException("Session is invalid or expired.");
        return UUID.fromString(id);
    }

    private UUID resolveOrgId(HttpServletRequest request) {
        String header = request.getHeader("X-Organization-Id");
        if (header == null) return null;
        try { return UUID.fromString(header); } catch (IllegalArgumentException e) { return null; }
    }

    private AuditLogFilter buildFilter(UUID actorId, String entityType, UUID entityId,
                                       UUID locationId, String action, Instant from, Instant to) {
        AuditLogFilter filter = new AuditLogFilter();
        filter.setActorId(actorId);
        filter.setEntityType(entityType);
        filter.setEntityId(entityId);
        filter.setLocationId(locationId);
        filter.setAction(action);
        filter.setFrom(from);
        filter.setTo(to);
        return filter;
    }
}

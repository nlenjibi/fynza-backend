package ecommerce.modules.audit.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.response.PaginatedResponse;
import ecommerce.common.util.PaginationUtils;
import ecommerce.modules.audit.dto.AuditLogDetailResponse;
import ecommerce.modules.audit.dto.AuditLogFilter;
import ecommerce.modules.audit.dto.AuditLogResponse;
import ecommerce.modules.audit.service.AuditLogExportService;
import ecommerce.modules.audit.service.AuditLogQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Audit log query, detail, and CSV export — Admin only")
public class AuditLogController {

    private final AuditLogQueryService  auditLogQueryService;
    private final AuditLogExportService auditLogExportService;

    @Operation(summary = "List audit logs", description = "Paginated, filtered list of audit entries ordered by most recent first.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Paginated results"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<AuditLogResponse>>> list(
            @Parameter(description = "Filter by actor public ID")
            @RequestParam(required = false) UUID actorPublicId,

            @Parameter(description = "Filter by entity type, e.g. PRODUCT")
            @RequestParam(required = false) String entityType,

            @Parameter(description = "Filter by entity public ID")
            @RequestParam(required = false) UUID entityPublicId,

            @Parameter(description = "Filter by one or more action codes")
            @RequestParam(required = false) List<String> actions,

            @Parameter(description = "Filter by status: SUCCESS or FAILED")
            @RequestParam(required = false) String status,

            @Parameter(description = "Start of date range (ISO-8601 instant)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,

            @Parameter(description = "End of date range (ISO-8601 instant)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,

            @Parameter(description = "Zero-based page number") @RequestParam(defaultValue = "0")  int page,
            @Parameter(description = "Page size (max 100)")    @RequestParam(defaultValue = "20") int size) {

        AuditLogFilter filter = buildFilter(actorPublicId, entityType, entityPublicId, actions, status, from, to);
        var pageable = PaginationUtils.of(page, size, 100, Sort.by("occurredAt").descending());

        Page<AuditLogResponse> result = auditLogQueryService.query(filter, pageable);
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved.", PaginatedResponse.from(result)));
    }

    @Operation(summary = "Get audit log detail", description = "Full entry detail including computed field-level diff.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Audit log detail"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Log entry not found")
    })
    @GetMapping("/{logId}")
    public ResponseEntity<ApiResponse<AuditLogDetailResponse>> getDetail(@PathVariable UUID logId) {
        AuditLogDetailResponse detail = auditLogQueryService.getById(logId);
        return ResponseEntity.ok(ApiResponse.success("Audit log detail retrieved.", detail));
    }

    @Operation(
            summary  = "Export audit logs as CSV",
            description = "Streams a CSV file of matching entries. Defaults to the last 90 days when no start date is provided.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "CSV stream"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Admin role required")
    })
    @GetMapping("/export")
    public ResponseEntity<StreamingResponseBody> export(
            @RequestParam(required = false) UUID actorPublicId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityPublicId,
            @RequestParam(required = false) List<String> actions,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {

        AuditLogFilter filter = buildFilter(actorPublicId, entityType, entityPublicId, actions, null, from, to);
        String filename = "audit-log-" + LocalDate.now() + ".csv";

        StreamingResponseBody body = auditLogExportService.exportCsv(filter);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(body);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private AuditLogFilter buildFilter(UUID actorPublicId, String entityType, UUID entityPublicId,
                                       List<String> actions, String status, Instant from, Instant to) {
        AuditLogFilter f = new AuditLogFilter();
        f.setActorPublicId(actorPublicId);
        f.setEntityType(entityType);
        f.setEntityPublicId(entityPublicId);
        f.setActions(actions);
        f.setStatus(status);
        f.setFrom(from);
        f.setTo(to);
        return f;
    }
}

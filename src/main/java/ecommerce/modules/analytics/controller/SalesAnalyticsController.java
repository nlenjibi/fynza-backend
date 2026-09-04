package ecommerce.modules.analytics.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.util.DateRangeValidator;
import ecommerce.modules.analytics.dto.SalesKpiResponse;
import ecommerce.modules.analytics.dto.TrendPoint;
import ecommerce.modules.analytics.dto.TrendResponse;
import ecommerce.modules.analytics.service.SalesAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Sales analytics dashboard endpoints — Admin only.
 *
 * Dual-mode: each endpoint accepts either {@code (year, month)} or {@code (fromDate, toDate)}.
 * When both are supplied the date-range params take precedence. Year/month default to the
 * current calendar month when omitted.
 */
@RestController
@RequestMapping("/api/v1/admin/analytics/sales")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Sales Analytics", description = "Sales KPI cards and revenue trend — Admin only")
public class SalesAnalyticsController {

    private final SalesAnalyticsService analyticsService;

    @Operation(
            summary = "Get sales KPI summary",
            description = "Returns 6 KPI cards (revenue, orders, AOV, active customers, refund rate, cancel rate) " +
                          "for the given month or custom date range.")
    @GetMapping("/kpi")
    public ResponseEntity<ApiResponse<SalesKpiResponse>> getKpiSummary(
            @Parameter(description = "Year (defaults to current year). Ignored when fromDate/toDate are supplied.")
            @RequestParam(required = false) Integer year,

            @Parameter(description = "Month 1–12 (defaults to current month). Ignored when fromDate/toDate are supplied.")
            @RequestParam(required = false) Integer month,

            @Parameter(description = "Start of an exact date range (inclusive). Takes precedence over year/month.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @Parameter(description = "End of an exact date range (inclusive). Takes precedence over year/month.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        if (fromDate != null || toDate != null) {
            DateRangeValidator.validate(fromDate, toDate);
            return ResponseEntity.ok(ApiResponse.success(
                    analyticsService.getKpiSummary(fromDate, toDate)));
        }

        int resolvedYear  = year  != null ? year  : LocalDate.now().getYear();
        int resolvedMonth = month != null ? month : LocalDate.now().getMonthValue();

        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getKpiSummary(resolvedYear, resolvedMonth)));
    }

    @Operation(
            summary = "Get revenue trend",
            description = "Returns a revenue trend bucketed at WEEK (≤42 days) or MONTH granularity " +
                          "for the given month or custom date range.")
    @GetMapping("/revenue-trend")
    public ResponseEntity<ApiResponse<TrendResponse<TrendPoint>>> getRevenueTrend(
            @Parameter(description = "Year (defaults to current year). Ignored when fromDate/toDate are supplied.")
            @RequestParam(required = false) Integer year,

            @Parameter(description = "Month 1–12 (defaults to current month). Ignored when fromDate/toDate are supplied.")
            @RequestParam(required = false) Integer month,

            @Parameter(description = "Start of an exact date range (inclusive). Takes precedence over year/month.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @Parameter(description = "End of an exact date range (inclusive).")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        if (fromDate != null || toDate != null) {
            DateRangeValidator.validate(fromDate, toDate);
            return ResponseEntity.ok(ApiResponse.success(
                    analyticsService.getRevenueTrend(fromDate, toDate)));
        }

        int resolvedYear  = year  != null ? year  : LocalDate.now().getYear();
        int resolvedMonth = month != null ? month : LocalDate.now().getMonthValue();

        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getRevenueTrend(resolvedYear, resolvedMonth)));
    }
}

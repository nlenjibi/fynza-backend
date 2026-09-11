package ecommerce.modules.analytics.service;

import ecommerce.modules.analytics.dto.SalesKpiResponse;
import ecommerce.modules.analytics.dto.TrendPoint;
import ecommerce.modules.analytics.dto.TrendResponse;

import java.time.LocalDate;

/**
 * Sales analytics — dual-mode contract mirroring OMS's EmployeeAnalyticsService pattern:
 * <ul>
 *   <li>Year/month overloads — pre-aggregated monthly data, fast and cheap.</li>
 *   <li>Date-range overloads — ad-hoc queries over the orders table, bucketed at
 *       WEEK or MONTH granularity depending on range length.</li>
 * </ul>
 * Date-range overloads take precedence over year/month when both are supplied in a request.
 */
public interface SalesAnalyticsService {

    SalesKpiResponse getKpiSummary(int year, int month);

    SalesKpiResponse getKpiSummary(LocalDate fromDate, LocalDate toDate);

    TrendResponse<TrendPoint> getRevenueTrend(int year, int month);

    TrendResponse<TrendPoint> getRevenueTrend(LocalDate fromDate, LocalDate toDate);
}

package ecommerce.modules.analytics.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Six core sales KPI cards for the admin analytics dashboard.
 * Each card carries a current-period value, a period-over-period change %, and a trend direction.
 */
@Getter
@Builder
public class SalesKpiResponse {

    /** Gross revenue in the period (sum of confirmed order totals). */
    private KpiCard<BigDecimal> totalRevenue;

    /** Number of orders placed in the period. */
    private KpiCard<Long> totalOrders;

    /** Average order value = totalRevenue / totalOrders. */
    private KpiCard<BigDecimal> averageOrderValue;

    /** Distinct customers who placed at least one order in the period. */
    private KpiCard<Long> activeCustomers;

    /** Refunded orders / total orders × 100. */
    private KpiCard<BigDecimal> refundRate;

    /** Cancelled orders / total orders × 100. */
    private KpiCard<BigDecimal> cancelRate;
}

package ecommerce.modules.analytics.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Generic trend container. {@code granularity} is WEEK for ranges ≤ 42 days,
 * MONTH otherwise — mirrors the OMS TrendPeriodAggregator convention.
 */
@Getter
@Builder
public class TrendResponse<T> {
    private String  granularity;
    private List<T> points;
}

package ecommerce.modules.analytics.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Generic KPI card carrying a typed value, a period-over-period change percentage,
 * and a trend direction (UP / DOWN / FLAT). Null change fields mean comparison data
 * is unavailable (e.g. first period on record).
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KpiCard<T> {
    private T          value;
    private BigDecimal changePct;
    private String     trendDirection;
}

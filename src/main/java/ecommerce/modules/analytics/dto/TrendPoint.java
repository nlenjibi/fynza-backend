package ecommerce.modules.analytics.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single data point in a sales trend series.
 */
@Getter
@Builder
public class TrendPoint {
    private LocalDate  date;
    private String     label;
    private BigDecimal revenue;
    private Long       orders;
    private BigDecimal averageOrderValue;
}

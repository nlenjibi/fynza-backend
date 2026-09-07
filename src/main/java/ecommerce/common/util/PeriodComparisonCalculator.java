package ecommerce.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Compares a date-ranged KPI metric against the immediately preceding period of equal length,
 * producing the {@code rateChangePct} and {@code trendDirection} values exposed on KPI cards.
 *
 * Port of OMS {@code PeriodComparisonCalculator} adapted for e-commerce sales analytics.
 */
public final class PeriodComparisonCalculator {

    public static final String TREND_UP   = "UP";
    public static final String TREND_DOWN = "DOWN";
    public static final String TREND_FLAT = "FLAT";

    private PeriodComparisonCalculator() {}

    /**
     * Returns {@code [previousFrom, previousTo]} — the period immediately before
     * {@code [fromDate, toDate]} of the same length in days.
     */
    public static LocalDate[] precedingPeriod(LocalDate fromDate, LocalDate toDate) {
        long lengthDays = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        LocalDate previousTo   = fromDate.minusDays(1);
        LocalDate previousFrom = previousTo.minusDays(lengthDays - 1);
        return new LocalDate[]{previousFrom, previousTo};
    }

    /**
     * Absolute change between two percentage values, rounded to 1 decimal place.
     * Returns {@code null} if either value is null.
     */
    public static BigDecimal rateChangePct(BigDecimal currentPct, BigDecimal previousPct) {
        if (currentPct == null || previousPct == null) return null;
        return currentPct.subtract(previousPct).setScale(1, RoundingMode.HALF_UP);
    }

    /**
     * Percentage change between two absolute values, e.g. revenue.
     * Returns {@code null} if either value is null or previous is zero.
     */
    public static BigDecimal valueChangePct(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null || previous.compareTo(BigDecimal.ZERO) == 0) return null;
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);
    }

    public static String trendDirection(BigDecimal changePct) {
        if (changePct == null) return null;
        int cmp = changePct.compareTo(BigDecimal.ZERO);
        if (cmp > 0) return TREND_UP;
        if (cmp < 0) return TREND_DOWN;
        return TREND_FLAT;
    }
}

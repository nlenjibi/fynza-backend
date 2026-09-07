package ecommerce.common.util;

import ecommerce.common.exception.BadRequestException;

import java.time.LocalDate;

public final class DateRangeValidator {

    private DateRangeValidator() {}

    public static void validate(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new BadRequestException(
                    "Both fromDate and toDate must be provided together.", "INVALID_DATE_RANGE");
        }
        if (fromDate.isAfter(toDate)) {
            throw new BadRequestException(
                    "fromDate must not be after toDate.", "INVALID_DATE_RANGE");
        }
    }
}

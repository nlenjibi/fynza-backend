package ecommerce.common.validation;

import java.math.BigDecimal;

/** Implemented by any DTO that carries an old/new price pair for price-drop validation. */
public interface PriceComparable {
    BigDecimal getOldPrice();
    BigDecimal getNewPrice();
}

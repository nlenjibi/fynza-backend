package ecommerce.modules.cart.event;

import java.math.BigDecimal;
import java.util.UUID;

public record CouponAppliedEvent(UUID cartPublicId, UUID userId, String couponCode, BigDecimal discountAmount) {}

package ecommerce.modules.cart.event;

import java.math.BigDecimal;
import java.util.UUID;

public record CartCheckedOutEvent(UUID cartPublicId, UUID userId, BigDecimal grandTotal, int itemCount) {}

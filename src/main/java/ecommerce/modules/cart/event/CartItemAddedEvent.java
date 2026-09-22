package ecommerce.modules.cart.event;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemAddedEvent(UUID cartPublicId, UUID userId, UUID productId, UUID variantId, int quantity, BigDecimal unitPrice) {}

package ecommerce.common.event.pricing;

import ecommerce.common.event.DomainEvent;
import ecommerce.modules.pricing.enums.SupportedCurrency;

import java.math.BigDecimal;
import java.util.UUID;

public record PriceActivatedEvent(
        UUID publicPriceId,
        UUID productId,
        UUID variantId,
        BigDecimal amount,
        SupportedCurrency currency
) implements DomainEvent {}

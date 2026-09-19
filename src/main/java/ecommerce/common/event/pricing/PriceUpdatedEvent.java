package ecommerce.common.event.pricing;

import ecommerce.common.event.DomainEvent;
import ecommerce.modules.pricing.enums.SupportedCurrency;

import java.math.BigDecimal;
import java.util.UUID;

public record PriceUpdatedEvent(
        UUID publicPriceId,
        UUID productId,
        UUID variantId,
        BigDecimal oldAmount,
        BigDecimal newAmount,
        SupportedCurrency currency
) implements DomainEvent {}

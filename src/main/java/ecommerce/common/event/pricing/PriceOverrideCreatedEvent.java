package ecommerce.common.event.pricing;

import ecommerce.common.event.DomainEvent;

import java.math.BigDecimal;
import java.util.UUID;

public record PriceOverrideCreatedEvent(
        UUID publicPriceId,
        UUID productId,
        BigDecimal oldAmount,
        BigDecimal newAmount,
        UUID approvedBy,
        String reason
) implements DomainEvent {}

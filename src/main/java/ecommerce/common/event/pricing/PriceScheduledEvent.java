package ecommerce.common.event.pricing;

import ecommerce.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record PriceScheduledEvent(
        UUID publicPriceId,
        UUID productId,
        UUID variantId,
        Instant validFrom,
        Instant validUntil
) implements DomainEvent {}

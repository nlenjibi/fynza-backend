package ecommerce.common.event.pricing;

import ecommerce.common.event.DomainEvent;

import java.util.UUID;

public record PriceDisabledEvent(
        UUID publicPriceId,
        UUID productId,
        UUID variantId
) implements DomainEvent {}

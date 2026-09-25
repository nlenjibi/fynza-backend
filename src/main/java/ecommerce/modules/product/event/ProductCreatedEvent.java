package ecommerce.modules.product.event;

import ecommerce.common.event.DomainEvent;

import java.util.UUID;

public record ProductCreatedEvent(UUID productId, Long sellerId) implements DomainEvent {}

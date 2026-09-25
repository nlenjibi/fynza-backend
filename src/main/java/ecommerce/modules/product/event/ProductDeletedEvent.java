package ecommerce.modules.product.event;

import ecommerce.common.event.DomainEvent;

import java.util.UUID;

public record ProductDeletedEvent(UUID productId) implements DomainEvent {}

package ecommerce.modules.product.event;

import ecommerce.common.event.DomainEvent;

import java.util.UUID;

public record ProductPublishedEvent(UUID productId) implements DomainEvent {}

package ecommerce.common.event;

/**
 * Marker interface for all Fynza domain events.
 * Events are Java records published via ApplicationEventPublisher and consumed
 * by @TransactionalEventListener(phase = AFTER_COMMIT) listeners, ensuring
 * side-effects (emails, notifications, stock updates) only run when the
 * triggering transaction has successfully committed.
 */
public interface DomainEvent {}

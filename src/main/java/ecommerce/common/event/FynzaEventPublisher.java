package ecommerce.common.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around Spring's ApplicationEventPublisher.
 * Services call this instead of injecting ApplicationEventPublisher directly,
 * keeping the event-publishing intent explicit and testable.
 *
 * Usage in a service:
 * <pre>
 *   eventPublisher.publish(new OrderPlacedEvent(...));
 * </pre>
 *
 * The event fires only after the surrounding transaction commits because all
 * listeners use @TransactionalEventListener(phase = AFTER_COMMIT).
 */
@Component
@RequiredArgsConstructor
public class FynzaEventPublisher {

    private final ApplicationEventPublisher publisher;

    public void publish(DomainEvent event) {
        publisher.publishEvent(event);
    }
}

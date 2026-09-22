package ecommerce.modules.cart.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartEventPublisher {

    private final ApplicationEventPublisher publisher;

    public void publish(Object event) {
        log.debug("Publishing cart event: {}", event.getClass().getSimpleName());
        publisher.publishEvent(event);
    }
}

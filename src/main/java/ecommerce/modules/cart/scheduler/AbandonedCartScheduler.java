package ecommerce.modules.cart.scheduler;

import ecommerce.modules.cart.entity.Cart;
import ecommerce.modules.cart.entity.CartStatus;
import ecommerce.modules.cart.event.CartAbandonedEvent;
import ecommerce.modules.cart.event.CartEventPublisher;
import ecommerce.modules.cart.event.CartExpiredEvent;
import ecommerce.modules.cart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AbandonedCartScheduler {

    private final CartRepository     cartRepository;
    private final CartEventPublisher eventPublisher;

    @Value("${cart.abandonment.days:7}")
    private int abandonedDays;

    @Value("${cart.cleanup.days:30}")
    private int cleanupDays;

    @Scheduled(cron = "${cart.abandonment.cron:0 0 2 * * *}")
    @Transactional
    public void markAbandonedCarts() {
        log.info("Starting abandoned cart marking job");
        Instant cutoff = Instant.now().minus(abandonedDays, ChronoUnit.DAYS);
        List<Cart> stale = cartRepository.findInactiveUserCarts(cutoff);
        for (Cart cart : stale) {
            cart.setStatus(CartStatus.ABANDONED);
            cartRepository.save(cart);
            eventPublisher.publish(new CartAbandonedEvent(
                    cart.getPublicId(), cart.getUserId(), null, null,
                    cart.getGrandTotal(), cart.getItems().size(), cart.getIsGuest()));
            log.info("Cart {} marked abandoned for user={}", cart.getPublicId(), cart.getUserId());
        }
        log.info("Marked {} carts as abandoned", stale.size());
    }

    @Scheduled(cron = "${cart.cleanup.cron:0 0 3 * * *}")
    @Transactional
    public void cleanupAbandonedCarts() {
        log.info("Starting abandoned cart cleanup job");
        Instant cutoff = Instant.now().minus(cleanupDays, ChronoUnit.DAYS);
        List<Cart> old = cartRepository.findAbandonedCartsOlderThan(cutoff);
        for (Cart cart : old) {
            cart.setStatus(CartStatus.EXPIRED);
            cartRepository.save(cart);
            log.info("Archived abandoned cart {} for user={}", cart.getPublicId(), cart.getUserId());
        }
        log.info("Archived {} abandoned carts", old.size());
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireGuestCarts() {
        log.info("Expiring guest carts");
        List<Cart> expired = cartRepository.findExpiredGuestCarts(Instant.now());
        for (Cart cart : expired) {
            cart.setStatus(CartStatus.EXPIRED);
            cartRepository.save(cart);
            eventPublisher.publish(new CartExpiredEvent(cart.getPublicId(), cart.getUserId(), true));
        }
        log.info("Expired {} guest carts", expired.size());
    }
}

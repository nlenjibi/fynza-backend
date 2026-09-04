package ecommerce.modules.cart.scheduler;

import ecommerce.modules.cart.entity.Cart;
import ecommerce.modules.cart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Abandoned Cart Scheduler
 * 
 * Handles cart lifecycle:
 * 1. Mark carts as abandoned after X days of inactivity
 * 2. Delete abandoned carts after Y days
 * 
 * Per hybrid cart guidelines:
 * - Abandoned carts remain in DB for analytics
 * - Automatically cleaned up via cron job
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AbandonedCartScheduler {

    private final CartRepository cartRepository;

    /**
     * Days after which cart is marked abandoned
     * Default: 7 days of no activity
     */
    @Value("${cart.abandonment.days:7}")
    private int abandonedDays;

    /**
     * Days after which abandoned cart is deleted
     * Default: 30 days after being marked abandoned
     */
    @Value("${cart.cleanup.days:30}")
    private int cleanupDays;

    /**
     * Cron job to mark carts as abandoned
     * Runs daily at 2:00 AM
     */
    @Scheduled(cron = "${cart.abandonment.cron:0 0 2 * * *}")
    @Transactional
    public void markAbandonedCarts() {
        log.info("Starting abandoned cart marking job");
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(abandonedDays);
        
        List<Cart> cartsToMarkAbandoned = cartRepository
                .findCartsNotCheckedOutAndNotAbandoned(cutoffDate);
        
        for (Cart cart : cartsToMarkAbandoned) {
            cart.setIsAbandoned(true);
            cart.setAbandonedAt(LocalDateTime.now());
            cartRepository.save(cart);
            
            log.info("Marked cart {} as abandoned for user {}", 
                    cart.getId(), cart.getUserId());
        }
        
        log.info("Marked {} carts as abandoned", cartsToMarkAbandoned.size());
    }

    /**
     * Cron job to delete old abandoned carts
     * Runs daily at 3:00 AM
     */
    @Scheduled(cron = "${cart.cleanup.cron:0 0 3 * * *}")
    @Transactional
    public void cleanupAbandonedCarts() {
        log.info("Starting abandoned cart cleanup job");
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(cleanupDays);
        
        List<Cart> cartsToDelete = cartRepository
                .findAbandonedCartsOlderThan(cutoffDate);
        
        for (Cart cart : cartsToDelete) {
            cartRepository.delete(cart);
            log.info("Deleted abandoned cart {} for user {}", 
                    cart.getId(), cart.getUserId());
        }
        
        log.info("Deleted {} abandoned carts", cartsToDelete.size());
    }

    /**
     * Update last activity timestamp when cart is modified
     */
    public void updateLastActivity(UUID userId) {
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cart.setLastActivityAt(LocalDateTime.now());
            cartRepository.save(cart);
        });
    }
}

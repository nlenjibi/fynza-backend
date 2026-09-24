package ecommerce.modules.notification.repository;

import ecommerce.modules.notification.entity.NotificationWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationWebhookEventRepository extends JpaRepository<NotificationWebhookEvent, Long> {

    Optional<NotificationWebhookEvent> findByProviderAndProviderEventId(String provider, String providerEventId);

    boolean existsByProviderAndProviderEventId(String provider, String providerEventId);
}

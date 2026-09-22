package ecommerce.modules.shipping.repository;

import ecommerce.modules.shipping.entity.ShippingWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShippingWebhookEventRepository extends JpaRepository<ShippingWebhookEvent, Long> {
    boolean existsByProviderAndProviderEventId(String provider, String providerEventId);
    Optional<ShippingWebhookEvent> findByProviderAndProviderEventId(String provider, String providerEventId);
}

package ecommerce.modules.payout.repository;

import ecommerce.modules.payout.entity.PayoutProviderEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PayoutProviderEventRepository extends JpaRepository<PayoutProviderEvent, Long> {

    boolean existsByProviderAndEventId(String provider, String eventId);

    Optional<PayoutProviderEvent> findByProviderAndEventId(String provider, String eventId);
}

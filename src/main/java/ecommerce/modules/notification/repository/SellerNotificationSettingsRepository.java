package ecommerce.modules.notification.repository;

import ecommerce.modules.notification.entity.SellerNotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerNotificationSettingsRepository extends JpaRepository<SellerNotificationSettings, UUID> {
    Optional<SellerNotificationSettings> findBySellerId(UUID sellerId);
}

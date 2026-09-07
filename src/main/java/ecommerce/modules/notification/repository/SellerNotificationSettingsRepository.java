package ecommerce.modules.notification.repository;

import ecommerce.modules.notification.entity.SellerNotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SellerNotificationSettingsRepository extends JpaRepository<SellerNotificationSettings, Long> {
    Optional<SellerNotificationSettings> findBySellerId(Long sellerId);
}

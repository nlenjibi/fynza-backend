package ecommerce.modules.notification.repository;

import ecommerce.common.base.BaseRepository;
import ecommerce.modules.notification.entity.SellerNotificationSettings;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerNotificationSettingsRepository extends BaseRepository<SellerNotificationSettings, UUID> {
    Optional<SellerNotificationSettings> findBySellerId(UUID sellerId);
}

package ecommerce.modules.notification.repository;

import ecommerce.modules.notification.entity.NotificationChannelConfig;
import ecommerce.modules.notification.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationChannelConfigRepository extends JpaRepository<NotificationChannelConfig, Long> {

    Optional<NotificationChannelConfig> findByNotificationType(NotificationType notificationType);
}

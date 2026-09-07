package ecommerce.modules.notification.repository;

import ecommerce.modules.notification.entity.NotificationTemplate;
import ecommerce.modules.notification.enums.NotificationChannel;
import ecommerce.modules.notification.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate> findByNotificationTypeAndChannel(
            NotificationType notificationType, NotificationChannel channel);

    Optional<NotificationTemplate> findByNotificationTypeAndChannelAndActiveTrue(
            NotificationType notificationType, NotificationChannel channel);
}

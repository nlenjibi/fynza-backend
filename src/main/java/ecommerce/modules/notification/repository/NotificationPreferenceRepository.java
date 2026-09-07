package ecommerce.modules.notification.repository;

import ecommerce.modules.notification.entity.NotificationPreference;
import ecommerce.modules.notification.enums.NotificationChannel;
import ecommerce.modules.notification.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    Optional<NotificationPreference> findByUserIdAndNotificationTypeAndChannel(
            UUID userId, NotificationType notificationType, NotificationChannel channel);

    List<NotificationPreference> findByUserId(UUID userId);
}

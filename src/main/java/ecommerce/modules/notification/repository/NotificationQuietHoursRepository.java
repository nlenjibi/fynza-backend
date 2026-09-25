package ecommerce.modules.notification.repository;

import ecommerce.modules.notification.entity.NotificationQuietHours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationQuietHoursRepository extends JpaRepository<NotificationQuietHours, Long> {

    Optional<NotificationQuietHours> findByUserId(UUID userId);
}

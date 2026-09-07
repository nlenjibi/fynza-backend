package ecommerce.modules.notification.repository;

import ecommerce.modules.notification.entity.NotificationDispatch;
import ecommerce.modules.notification.enums.NotificationChannel;
import ecommerce.modules.notification.enums.NotificationStatus;
import ecommerce.modules.notification.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationDispatchRepository extends JpaRepository<NotificationDispatch, Long> {

    List<NotificationDispatch> findByNotificationEventId(UUID eventId);

    @Query("SELECT d FROM NotificationDispatch d " +
           "WHERE d.status = :status AND d.scheduledAt <= :now " +
           "ORDER BY d.scheduledAt ASC")
    List<NotificationDispatch> findDueForRetry(
            @Param("status") NotificationStatus status,
            @Param("now") Instant now);

    boolean existsByRecipientIdAndNotificationTypeAndScheduledAtAfter(
            UUID recipientId, NotificationType notificationType, Instant cutoff);

    boolean existsByChannelAndNotificationTypeAndSourceEntityId(
            NotificationChannel channel, NotificationType notificationType, UUID sourceEntityId);
}

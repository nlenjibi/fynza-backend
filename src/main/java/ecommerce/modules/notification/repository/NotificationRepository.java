package ecommerce.modules.notification.repository;

import ecommerce.common.base.BaseRepository;
import ecommerce.modules.notification.entity.Notification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends BaseRepository<Notification, UUID> {

    List<Notification> findByUserId(UUID userId);

    List<Notification> findByUserIdAndIsReadFalse(UUID userId);
}

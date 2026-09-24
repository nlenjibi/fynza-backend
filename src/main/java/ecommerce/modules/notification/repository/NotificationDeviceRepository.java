package ecommerce.modules.notification.repository;

import ecommerce.modules.notification.entity.NotificationDevice;
import ecommerce.modules.notification.enums.DeviceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationDeviceRepository extends JpaRepository<NotificationDevice, Long> {

    List<NotificationDevice> findByUserIdAndStatus(UUID userId, DeviceStatus status);

    Optional<NotificationDevice> findByDeviceToken(String deviceToken);

    Optional<NotificationDevice> findByPublicIdAndUserId(UUID publicId, UUID userId);

    @Modifying
    @Query("UPDATE NotificationDevice d SET d.status = 'INACTIVE' WHERE d.deviceToken = :token")
    void invalidateByToken(@Param("token") String deviceToken);
}

package ecommerce.modules.notification.repository;

import ecommerce.modules.notification.entity.NotificationPreferenceActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationPreferenceActivityRepository extends JpaRepository<NotificationPreferenceActivity, UUID> {

    Page<NotificationPreferenceActivity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<NotificationPreferenceActivity> findByUserIdAndSettingNameOrderByCreatedAtDesc(
            UUID userId, String settingName, Pageable pageable);

    List<NotificationPreferenceActivity> findByUserIdOrderByCreatedAtDesc(UUID userId);
}

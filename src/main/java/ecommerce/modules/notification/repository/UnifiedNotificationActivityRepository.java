package ecommerce.modules.notification.repository;

import ecommerce.modules.notification.entity.UnifiedNotificationActivity;
import ecommerce.modules.notification.entity.UnifiedNotificationActivity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface UnifiedNotificationActivityRepository extends JpaRepository<UnifiedNotificationActivity, UUID> {

    Page<UnifiedNotificationActivity> findByUserIdAndCategoryOrderByCreatedAtDesc(
            UUID userId, Category category, Pageable pageable);

    Page<UnifiedNotificationActivity> findByUserIdAndIsReadOrderByCreatedAtDesc(
            UUID userId, Boolean isRead, Pageable pageable);

    Page<UnifiedNotificationActivity> findByUserIdAndNotificationTypeOrderByCreatedAtDesc(
            UUID userId, NotificationType type, Pageable pageable);

    Page<UnifiedNotificationActivity> findByUserIdAndIsPinnedTrueOrderByCreatedAtDesc(
            UUID userId, Pageable pageable);

    @Query("SELECT n FROM UnifiedNotificationActivity n WHERE n.userId = :userId AND n.isRead = false")
    Page<UnifiedNotificationActivity> findUnread(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT COUNT(n) FROM UnifiedNotificationActivity n WHERE n.userId = :userId AND n.isRead = false AND n.category = :category")
    long countUnreadByCategory(@Param("userId") UUID userId, @Param("category") Category category);

    @Query("SELECT n.notificationType, COUNT(n) FROM UnifiedNotificationActivity n WHERE n.userId = :userId AND n.createdAt >= :since GROUP BY n.notificationType")
    List<Object[]> getTypeSummary(@Param("userId") UUID userId, @Param("since") LocalDateTime since);

    @Query("SELECT n FROM UnifiedNotificationActivity n WHERE n.userId = :userId AND n.senderType = :senderType ORDER BY n.createdAt DESC")
    Page<UnifiedNotificationActivity> findBySenderType(@Param("userId") UUID userId, @Param("senderType") String senderType, Pageable pageable);
}

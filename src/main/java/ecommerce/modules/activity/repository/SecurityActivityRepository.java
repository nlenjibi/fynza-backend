package ecommerce.modules.activity.repository;

import ecommerce.modules.activity.entity.SecurityActivity;
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
public interface SecurityActivityRepository extends JpaRepository<SecurityActivity, UUID> {

    Page<SecurityActivity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<SecurityActivity> findByUserIdAndActivityTypeOrderByCreatedAtDesc(
            UUID userId, SecurityActivity.SecurityActivityType type, Pageable pageable);

    @Query("SELECT a FROM SecurityActivity a WHERE a.userId = :userId AND a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<SecurityActivity> findRecentActivities(@Param("userId") UUID userId, @Param("since") LocalDateTime since, Pageable pageable);

    @Query("SELECT a.activityType, COUNT(a) FROM SecurityActivity a WHERE a.userId = :userId AND a.createdAt >= :since GROUP BY a.activityType")
    List<Object[]> getActivitySummary(@Param("userId") UUID userId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM SecurityActivity a WHERE a.userId = :userId AND a.activityType = 'LOGIN_FAILED' AND a.createdAt >= :since")
    long countFailedLogins(@Param("userId") UUID userId, @Param("since") LocalDateTime since);
}

package ecommerce.modules.activity.repository;

import ecommerce.modules.activity.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for ActivityLog entity.
 * 
 * Provides data access for:
 * - General activity logs (business events)
 * - Audit logs (technical CRUD operations)
 */
@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {

    // =================================================================
    // GENERAL ACTIVITY QUERIES
    // =================================================================

    Page<ActivityLog> findByUserId(UUID userId, Pageable pageable);

    Page<ActivityLog> findByActivityType(ActivityLog.ActivityType activityType, Pageable pageable);

    @Query("SELECT a FROM ActivityLog a WHERE a.createdAt BETWEEN :startDate AND :endDate")
    Page<ActivityLog> findByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    @Query("SELECT a FROM ActivityLog a WHERE a.entityType = :entityType AND a.entityId = :entityId")
    List<ActivityLog> findByEntity(String entityType, UUID entityId);

    @Query("SELECT COUNT(a) FROM ActivityLog a WHERE a.activityType = :type AND a.createdAt >= :since")
    long countByTypeSince(ActivityLog.ActivityType type, LocalDateTime since);

    @Query("SELECT a.activityType, COUNT(a) FROM ActivityLog a WHERE a.createdAt >= :since GROUP BY a.activityType")
    List<Object[]> countByActivityTypeGrouped(LocalDateTime since);

    // =================================================================
    // AUDIT LOG QUERIES (from AuditLog)
    // =================================================================

    Page<ActivityLog> findByAction(String action, Pageable pageable);

    Page<ActivityLog> findByStatus(String status, Pageable pageable);

    @Query("SELECT a FROM ActivityLog a WHERE a.userId = :userId AND a.createdAt >= :startDate AND a.createdAt <= :endDate")
    List<ActivityLog> findByUserIdAndDateRange(
            UUID userId, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT a FROM ActivityLog a WHERE a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.createdAt DESC")
    List<ActivityLog> findByEntityTypeAndEntityId(String entityType, UUID entityId);
}

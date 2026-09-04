package ecommerce.modules.activity.repository;

import ecommerce.modules.activity.entity.OrderActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for OrderActivityLog entity.
 * 
 * Provides data access operations for order-specific activity logs.
 */
@Repository
public interface OrderActivityLogRepository extends JpaRepository<OrderActivityLog, UUID> {

    /**
     * Find all activities for a specific order, ordered by creation date descending.
     */
    Page<OrderActivityLog> findByOrderIdOrderByCreatedAtDesc(UUID orderId, Pageable pageable);

    /**
     * Find all activities for a specific user.
     */
    Page<OrderActivityLog> findByUserId(UUID userId, Pageable pageable);

    /**
     * Find activities by order ID and activity type.
     */
    @Query("SELECT a FROM OrderActivityLog a WHERE a.orderId = :orderId AND a.activityType = :type ORDER BY a.createdAt DESC")
    List<OrderActivityLog> findByOrderIdAndType(UUID orderId, OrderActivityLog.OrderActivityType type);

    /**
     * Find recent activities since a given timestamp.
     */
    @Query("SELECT a FROM OrderActivityLog a WHERE a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<OrderActivityLog> findRecentActivities(LocalDateTime since, Pageable pageable);

    /**
     * Get activity count grouped by activity type for a specific order.
     */
    @Query("SELECT a.activityType, COUNT(a) FROM OrderActivityLog a WHERE a.orderId = :orderId GROUP BY a.activityType")
    List<Object[]> countByActivityTypeForOrder(UUID orderId);
}

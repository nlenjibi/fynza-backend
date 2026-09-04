package ecommerce.modules.activity.repository;

import ecommerce.modules.activity.entity.PaymentActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for PaymentActivityLog entity.
 * 
 * Provides data access operations for payment-specific activity logs.
 */
@Repository
public interface PaymentActivityLogRepository extends JpaRepository<PaymentActivityLog, UUID> {

    /**
     * Find all payment activities for a specific user, ordered by creation date descending.
     */
    Page<PaymentActivityLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Find payment activities by user ID and activity type.
     */
    @Query("SELECT a FROM PaymentActivityLog a WHERE a.userId = :userId AND a.activityType = :type ORDER BY a.createdAt DESC")
    Page<PaymentActivityLog> findByUserIdAndType(
            @Param("userId") UUID userId, 
            @Param("type") PaymentActivityLog.PaymentActivityType type, 
            Pageable pageable);

    /**
     * Find payment activities by user ID within a date range.
     */
    @Query("SELECT a FROM PaymentActivityLog a WHERE a.userId = :userId AND a.createdAt BETWEEN :start AND :end ORDER BY a.createdAt DESC")
    Page<PaymentActivityLog> findByUserIdAndDateRange(
            @Param("userId") UUID userId, 
            @Param("start") LocalDateTime start, 
            @Param("end") LocalDateTime end, 
            Pageable pageable);

    /**
     * Get payment activity summary grouped by activity type.
     */
    @Query("SELECT a.activityType, COUNT(a) FROM PaymentActivityLog a WHERE a.userId = :userId AND a.createdAt >= :since GROUP BY a.activityType")
    List<Object[]> getActivitySummary(@Param("userId") UUID userId, @Param("since") LocalDateTime since);

    /**
     * Get total amount spent by user since a given time.
     */
    @Query("SELECT COALESCE(SUM(a.amount), 0) FROM PaymentActivityLog a WHERE a.userId = :userId AND a.activityType = 'PAYMENT_SUCCESS' AND a.createdAt >= :since")
    BigDecimal getTotalSpent(@Param("userId") UUID userId, @Param("since") LocalDateTime since);

    /**
     * Find activities by payment ID.
     */
    Page<PaymentActivityLog> findByPaymentIdOrderByCreatedAtDesc(UUID paymentId, Pageable pageable);
}

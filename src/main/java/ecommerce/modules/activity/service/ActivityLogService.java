package ecommerce.modules.activity.service;

import ecommerce.modules.activity.entity.ActivityLog;
import ecommerce.modules.activity.entity.OrderActivityLog;
import ecommerce.modules.activity.entity.PaymentActivityLog;
import ecommerce.modules.activity.repository.ActivityLogRepository;
import ecommerce.modules.activity.repository.OrderActivityLogRepository;
import ecommerce.modules.activity.repository.PaymentActivityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * =================================================================
 * ACTIVITY LOG SERVICE
 * =================================================================
 * 
 * PURPOSE:
 * Unified service for all activity logging operations across the application.
 * Handles:
 * - General activity logs (business events)
 * - Order-specific activity logs
 * - Payment-specific activity logs
 * - Audit logs (technical CRUD operations)
 * 
 * LAYER: Service (Business Logic)
 * 
 * ARCHITECTURE:
 * All activity/audit data is consolidated into the ActivityLog entity
 * and this single service for unified access.
 * 
 * @author Fynza Backend Team
 * @version 2.1
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final OrderActivityLogRepository orderActivityLogRepository;
    private final PaymentActivityLogRepository paymentActivityLogRepository;

    // =================================================================
    // GENERAL ACTIVITY LOG METHODS
    // =================================================================

    /**
     * Logs a general activity asynchronously.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logActivity(ActivityLog.ActivityType type, String description, 
                            String entityType, UUID entityId, UUID userId, 
                            String userName, String userEmail, String ipAddress) {
        ActivityLog activityLog = ActivityLog.builder()
                .userId(userId)
                .userName(userName)
                .userEmail(userEmail)
                .activityType(type)
                .description(description)
                .entityType(entityType)
                .entityId(entityId)
                .ipAddress(ipAddress)
                .createdAt(LocalDateTime.now())
                .build();
        activityLogRepository.save(activityLog);
    }

    /**
     * Logs a general activity synchronously (for transactional contexts).
     */
    @Transactional
    public ActivityLog logActivitySync(ActivityLog.ActivityType type, String description, 
                                       String entityType, UUID entityId, UUID userId, 
                                       String userName, String userEmail, String ipAddress) {
        ActivityLog activityLog = ActivityLog.builder()
                .userId(userId)
                .userName(userName)
                .userEmail(userEmail)
                .activityType(type)
                .description(description)
                .entityType(entityType)
                .entityId(entityId)
                .ipAddress(ipAddress)
                .createdAt(LocalDateTime.now())
                .build();
        return activityLogRepository.save(activityLog);
    }

    /**
     * Retrieves all activities with pagination.
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> getAllActivities(Pageable pageable) {
        return activityLogRepository.findAll(pageable);
    }

    /**
     * Retrieves all activities for a specific user.
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> getActivitiesByUser(UUID userId, Pageable pageable) {
        return activityLogRepository.findByUserId(userId, pageable);
    }

    /**
     * Retrieves all activities of a specific type.
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> getActivitiesByType(ActivityLog.ActivityType type, Pageable pageable) {
        return activityLogRepository.findByActivityType(type, pageable);
    }

    /**
     * Retrieves activities within a date range.
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> getActivitiesByDateRange(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        return activityLogRepository.findByDateRange(start, end, pageable);
    }

    /**
     * Retrieves the history of changes for a specific entity.
     */
    @Transactional(readOnly = true)
    public List<ActivityLog> getEntityHistory(String entityType, UUID entityId) {
        return activityLogRepository.findByEntity(entityType, entityId);
    }

    /**
     * Gets activity summary statistics since a given time.
     */
    @Transactional(readOnly = true)
    public List<Object[]> getActivitySummary(LocalDateTime since) {
        return activityLogRepository.countByActivityTypeGrouped(since);
    }

    // =================================================================
    // AUDIT LOG METHODS (from AuditLog)
    // =================================================================

    /**
     * Logs an audit entry asynchronously.
     * 
     * @param auditLog The pre-built audit log entry
     * @return The saved audit log
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAudit(ActivityLog auditLog) {
        try {
            auditLog.setCreatedAt(LocalDateTime.now());
            activityLogRepository.save(auditLog);
            log.debug("Audit log persisted: {} - {}", auditLog.getAction(), auditLog.getStatus());
        } catch (Exception e) {
            log.error("Failed to persist audit log: {}", e.getMessage(), e);
        }
    }

    /**
     * Logs an audit action asynchronously.
     * 
     * @param userId      The user performing the action
     * @param username   The username
     * @param action      The action performed (CREATE, UPDATE, DELETE, etc.)
     * @param entityType The type of entity affected
     * @param entityId   The ID of the affected entity
     * @param status     The outcome (SUCCESS, FAILURE, ATTEMPTED)
     * @param details    Additional details
     * @param ipAddress  The client's IP address
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAuditAction(UUID userId, String username, String action, String entityType, 
                                UUID entityId, String status, String details, String ipAddress) {
        try {
            ActivityLog auditLog = ActivityLog.builder()
                    .userId(userId)
                    .userName(username)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .status(status)
                    .description(details)
                    .ipAddress(ipAddress)
                    .createdAt(LocalDateTime.now())
                    .build();
            activityLogRepository.save(auditLog);
            log.debug("Audit action logged: {} by user {}", action, username);
        } catch (Exception e) {
            log.error("Failed to log audit action: {}", e.getMessage(), e);
        }
    }

    /**
     * Logs an audit action synchronously.
     */
    @Transactional
    public ActivityLog logAuditActionSync(UUID userId, String username, String action, String entityType, 
                                          UUID entityId, String status, String details, String ipAddress) {
        ActivityLog auditLog = ActivityLog.builder()
                .userId(userId)
                .userName(username)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .status(status)
                .description(details)
                .ipAddress(ipAddress)
                .createdAt(LocalDateTime.now())
                .build();
        return activityLogRepository.save(auditLog);
    }

    /**
     * Retrieves audit logs for a specific user.
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> getAuditLogsByUserId(UUID userId, Pageable pageable) {
        return activityLogRepository.findByUserId(userId, pageable);
    }

    /**
     * Retrieves audit logs by action type.
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> getAuditLogsByAction(String action, Pageable pageable) {
        return activityLogRepository.findByAction(action, pageable);
    }

    /**
     * Retrieves audit logs by status.
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> getAuditLogsByStatus(String status, Pageable pageable) {
        return activityLogRepository.findByStatus(status, pageable);
    }

    /**
     * Retrieves audit logs within a date range.
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return activityLogRepository.findByDateRange(startDate, endDate, pageable);
    }

    /**
     * Retrieves audit logs for a specific entity.
     */
    @Transactional(readOnly = true)
    public List<ActivityLog> getAuditLogsForEntity(String entityType, UUID entityId) {
        return activityLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    // =================================================================
    // ORDER ACTIVITY LOG METHODS
    // =================================================================

    /**
     * Logs an order activity asynchronously.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logOrderActivity(UUID orderId, OrderActivityLog.OrderActivityType type, 
                                 String description, UUID userId, String oldValue, 
                                 String newValue, String ipAddress) {
        OrderActivityLog activity = OrderActivityLog.builder()
                .orderId(orderId)
                .userId(userId)
                .activityType(type)
                .description(description)
                .oldValue(oldValue)
                .newValue(newValue)
                .ipAddress(ipAddress)
                .createdAt(LocalDateTime.now())
                .build();
        orderActivityLogRepository.save(activity);
    }

    /**
     * Retrieves all activities for a specific order.
     */
    @Transactional(readOnly = true)
    public Page<OrderActivityLog> getOrderActivities(UUID orderId, Pageable pageable) {
        return orderActivityLogRepository.findByOrderIdOrderByCreatedAtDesc(orderId, pageable);
    }

    /**
     * Retrieves activities for a specific user.
     */
    @Transactional(readOnly = true)
    public Page<OrderActivityLog> getOrderActivitiesByUser(UUID userId, Pageable pageable) {
        return orderActivityLogRepository.findByUserId(userId, pageable);
    }

    /**
     * Retrieves activities by order ID and activity type.
     */
    @Transactional(readOnly = true)
    public List<OrderActivityLog> getOrderActivitiesByType(UUID orderId, OrderActivityLog.OrderActivityType type) {
        return orderActivityLogRepository.findByOrderIdAndType(orderId, type);
    }

    /**
     * Retrieves recent order activities.
     */
    @Transactional(readOnly = true)
    public List<OrderActivityLog> getRecentOrderActivities(int limit) {
        return orderActivityLogRepository.findRecentActivities(
                LocalDateTime.now().minusHours(24), Pageable.ofSize(limit));
    }

    /**
     * Gets activity summary for a specific order.
     */
    @Transactional(readOnly = true)
    public List<Object[]> getOrderActivitySummary(UUID orderId) {
        return orderActivityLogRepository.countByActivityTypeForOrder(orderId);
    }

    // =================================================================
    // PAYMENT ACTIVITY LOG METHODS
    // =================================================================

    /**
     * Logs a payment activity asynchronously.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPaymentActivity(UUID userId, PaymentActivityLog.PaymentActivityType type, 
                                  String description, BigDecimal amount, String paymentMethod,
                                  String status, UUID paymentId, String ipAddress) {
        PaymentActivityLog activity = PaymentActivityLog.builder()
                .userId(userId)
                .paymentId(paymentId)
                .activityType(type)
                .description(description)
                .amount(amount)
                .paymentMethod(paymentMethod)
                .status(status)
                .ipAddress(ipAddress)
                .createdAt(LocalDateTime.now())
                .build();
        paymentActivityLogRepository.save(activity);
        log.debug("Payment activity logged: {} for user: {}", type, userId);
    }

    /**
     * Retrieves all payment activities for a specific user.
     */
    @Transactional(readOnly = true)
    public Page<PaymentActivityLog> getUserPaymentActivities(UUID userId, Pageable pageable) {
        return paymentActivityLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Retrieves payment activities by user ID and activity type.
     */
    @Transactional(readOnly = true)
    public Page<PaymentActivityLog> getPaymentActivitiesByType(UUID userId, PaymentActivityLog.PaymentActivityType type, Pageable pageable) {
        return paymentActivityLogRepository.findByUserIdAndType(userId, type, pageable);
    }

    /**
     * Retrieves payment activities within a date range.
     */
    @Transactional(readOnly = true)
    public Page<PaymentActivityLog> getPaymentActivitiesByDateRange(UUID userId, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        return paymentActivityLogRepository.findByUserIdAndDateRange(userId, start, end, pageable);
    }

    /**
     * Gets payment activity summary for a user.
     */
    @Transactional(readOnly = true)
    public List<Object[]> getPaymentActivitySummary(UUID userId, int days) {
        return paymentActivityLogRepository.getActivitySummary(userId, LocalDateTime.now().minusDays(days));
    }

    /**
     * Gets total amount spent by user within a time period.
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalSpent(UUID userId, int days) {
        return paymentActivityLogRepository.getTotalSpent(userId, LocalDateTime.now().minusDays(days));
    }
}

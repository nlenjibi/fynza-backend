package ecommerce.modules.order.repository;

import ecommerce.common.base.BaseRepository;
import ecommerce.common.enums.PaymentMethod;
import ecommerce.modules.order.entity.Order;
import ecommerce.common.enums.OrderStatus;
import ecommerce.modules.order.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends BaseRepository<Order, UUID> {
    
    @EntityGraph(attributePaths = {"customer", "orderItems", "orderItems.product", "shippingAddress", "billingAddress"})
    Page<Order> findByCustomerId(UUID customerId, Pageable pageable);
    
    @EntityGraph(attributePaths = {"customer", "orderItems", "orderItems.product", "shippingAddress", "billingAddress"})
    Optional<Order> findByOrderNumber(String orderNumber);
    
    @EntityGraph(attributePaths = {"customer", "orderItems", "orderItems.product", "shippingAddress", "billingAddress"})
    Optional<Order> findById(UUID id);
    
    @EntityGraph(attributePaths = {"customer", "orderItems", "orderItems.product", "shippingAddress", "billingAddress"})
    Page<Order> findAll(Pageable pageable);
    
    long countByStatus(OrderStatus status);
    
    @EntityGraph(attributePaths = {"customer", "orderItems", "orderItems.product", "shippingAddress", "billingAddress"})
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM Order o JOIN o.orderItems oi WHERE o.customer.id = :customerId AND oi.product.id = :productId AND o.isActive = true")
    boolean existsByCustomerIdAndProductId(@Param("customerId") UUID customerId, @Param("productId") UUID productId);
    
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.product WHERE o.customer.id IN :customerIds")
    List<Order> findByCustomerIdIn(@Param("customerIds") List<UUID> customerIds);
    
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.product WHERE o.id IN :orderIds")
    List<Order> findByIdIn(@Param("orderIds") List<UUID> orderIds);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.paymentStatus = :paymentStatus AND o.isActive = true")
    long countByPaymentStatusAndIsActiveTrue(@Param("paymentStatus") PaymentStatus paymentStatus);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'COMPLETED' OR o.status = 'PAID'")
    BigDecimal calculateTotalRevenue();

    @Query("SELECT DISTINCT o FROM Order o JOIN o.orderItems oi JOIN oi.product p WHERE p.seller.id = :sellerId")
    @EntityGraph(attributePaths = {"customer", "orderItems", "orderItems.product", "shippingAddress", "billingAddress"})
    Page<Order> findBySellerId(@Param("sellerId") UUID sellerId, Pageable pageable);

    @Query("SELECT DISTINCT o FROM Order o JOIN o.orderItems oi JOIN oi.product p WHERE p.seller.id = :sellerId")
    @EntityGraph(attributePaths = {"customer", "orderItems", "orderItems.product", "shippingAddress", "billingAddress"})
    List<Order> findBySellerId(@Param("sellerId") UUID sellerId);

    @Query("SELECT o.status, COUNT(o) FROM Order o WHERE o.isActive = true GROUP BY o.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT o FROM Order o WHERE o.isActive = true ORDER BY o.createdAt DESC")
    List<Order> findRecentOrders(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT o FROM Order o JOIN o.customer c WHERE c.id = :customerId " +
           "AND (:status IS NULL OR o.status = :status) " +
           "AND (:dateFrom IS NULL OR o.createdAt >= :dateFrom) " +
           "AND (:dateTo IS NULL OR o.createdAt <= :dateTo) " +
           "AND (:minAmount IS NULL OR o.totalAmount >= :minAmount) " +
           "AND (:maxAmount IS NULL OR o.totalAmount <= :maxAmount) " +
           "AND (:query IS NULL OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Order> searchOrders(
            @Param("customerId") UUID customerId,
            @Param("status") String status,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("query") String query,
            Pageable pageable
    );

    @Query("SELECT o FROM Order o JOIN o.orderItems oi JOIN oi.product p " +
           "WHERE p.seller.id = :sellerId " +
           "AND (:status IS NULL OR o.status = :status) " +
           "AND (:dateFrom IS NULL OR o.createdAt >= :dateFrom) " +
           "AND (:dateTo IS NULL OR o.createdAt <= :dateTo) " +
           "AND (:query IS NULL OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(o.customer.firstName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(o.customer.lastName) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Order> findSellerOrdersWithFilters(
            @Param("sellerId") UUID sellerId,
            @Param("status") OrderStatus status,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            @Param("query") String query,
            Pageable pageable
    );

    @Query("SELECT o.status, COUNT(o) FROM Order o JOIN o.orderItems oi JOIN oi.product p " +
           "WHERE p.seller.id = :sellerId GROUP BY o.status")
    List<Object[]> countSellerOrdersByStatusGrouped(@Param("sellerId") UUID sellerId);

    @Query("SELECT o FROM Order o WHERE o.isActive = true " +
           "AND (:status IS NULL OR o.status = :status) " +
           "AND (:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus) " +
           "AND (:dateFrom IS NULL OR o.createdAt >= :dateFrom) " +
           "AND (:dateTo IS NULL OR o.createdAt <= :dateTo) " +
           "AND (:minAmount IS NULL OR o.totalAmount >= :minAmount) " +
           "AND (:maxAmount IS NULL OR o.totalAmount <= :maxAmount) " +
           "AND (:query IS NULL OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Order> searchOrdersAdmin(
            @Param("status") OrderStatus status,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("query") String query,
            Pageable pageable
    );

    @Query("SELECT o FROM Order o WHERE o.isActive = true " +
           "AND (:status IS NULL OR o.status = :status) " +
           "AND (:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus) " +
           "AND (:dateFrom IS NULL OR o.createdAt >= :dateFrom) " +
           "AND (:dateTo IS NULL OR o.createdAt <= :dateTo) " +
           "AND (:minAmount IS NULL OR o.totalAmount >= :minAmount) " +
           "AND (:maxAmount IS NULL OR o.totalAmount <= :maxAmount) " +
           "AND (:query IS NULL OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Order> searchOrdersAdminForExport(
            @Param("status") OrderStatus status,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("query") String query
    );

    @Query("SELECT o.paymentMethod, COUNT(o), COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
            "WHERE o.paymentStatus = 'PAID' AND o.isActive = true " +
            "GROUP BY o.paymentMethod")
    List<Object[]> getPaymentMethodBreakdown();

    @Query("SELECT o.paymentMethod, COUNT(o), COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
            "WHERE o.paymentStatus = 'PAID' AND o.isActive = true " +
            "AND o.createdAt >= :startDate " +
            "GROUP BY o.paymentMethod")
    List<Object[]> getPaymentMethodBreakdownSince(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
            "WHERE o.paymentStatus = 'PAID' AND o.isActive = true " +
            "AND o.createdAt >= :startDate AND o.createdAt < :endDate")
    BigDecimal getRevenueSince(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
            "WHERE o.paymentStatus = 'PENDING' AND o.isActive = true")
    BigDecimal getPendingPayments();
}

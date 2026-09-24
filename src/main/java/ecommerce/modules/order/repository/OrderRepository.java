package ecommerce.modules.order.repository;

import ecommerce.common.enums.OrderStatus;
import ecommerce.modules.order.entity.Order;
import ecommerce.modules.order.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByPublicId(UUID publicId);

    Optional<Order> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    Page<Order> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    long countByStatus(OrderStatus status);

    long countByPaymentStatus(PaymentStatus paymentStatus);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.paymentStatus = 'PAID' AND o.isActive = true")
    BigDecimal sumTotalRevenue();

    @Query("SELECT COALESCE(AVG(o.totalAmount), 0) FROM Order o WHERE o.isActive = true")
    BigDecimal avgOrderValue();

    @Query("SELECT o FROM Order o WHERE o.isActive = true ORDER BY o.createdAt DESC")
    List<Order> findRecentOrders(Pageable pageable);

    @Query("""
            SELECT o FROM Order o
            WHERE o.isActive = true
              AND (:status IS NULL OR o.status = :status)
              AND (:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus)
              AND (:query IS NULL OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<Order> searchAdmin(
            @Param("status") OrderStatus status,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("query") String query,
            Pageable pageable
    );

    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM Order o JOIN o.orderItems oi WHERE o.customerId = :customerId AND oi.productId = :productId AND o.isActive = true")
    boolean existsByCustomerIdAndProductId(@org.springframework.data.repository.query.Param("customerId") UUID customerId, @org.springframework.data.repository.query.Param("productId") UUID productId);

    @Query("""
            SELECT o FROM Order o
            WHERE o.customerId = :customerId
              AND (:status IS NULL OR o.status = :status)
              AND (:query IS NULL OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY o.createdAt DESC
            """)
    Page<Order> searchByCustomer(
            @Param("customerId") UUID customerId,
            @Param("status") OrderStatus status,
            @Param("query") String query,
            Pageable pageable
    );
}

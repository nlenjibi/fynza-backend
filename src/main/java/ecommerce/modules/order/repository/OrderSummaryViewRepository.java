package ecommerce.modules.order.repository;

import ecommerce.modules.order.entity.OrderSummaryView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderSummaryViewRepository
        extends JpaRepository<OrderSummaryView, Long>, JpaSpecificationExecutor<OrderSummaryView> {

    Page<OrderSummaryView> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    Page<OrderSummaryView> findByStatus(String status, Pageable pageable);

    @Query(value = """
            SELECT os.* FROM v_order_summary os
            WHERE EXISTS (
                SELECT 1 FROM seller_orders so
                WHERE so.order_id = os.id AND so.seller_id = :sellerId
            )
            ORDER BY os.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM v_order_summary os
            WHERE EXISTS (
                SELECT 1 FROM seller_orders so
                WHERE so.order_id = os.id AND so.seller_id = :sellerId
            )
            """,
            nativeQuery = true)
    Page<OrderSummaryView> findBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);

    @Query("""
            SELECT o FROM OrderSummaryView o
            WHERE o.isActive = true
              AND (:status IS NULL OR o.status = :status)
              AND (:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus)
              AND (:query IS NULL OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    Page<OrderSummaryView> searchAdmin(
            @Param("status") String status,
            @Param("paymentStatus") String paymentStatus,
            @Param("query") String query,
            Pageable pageable
    );
}

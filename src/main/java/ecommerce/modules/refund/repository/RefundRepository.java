package ecommerce.modules.refund.repository;

import ecommerce.common.enums.RefundStatus;
import ecommerce.modules.refund.entity.Refund;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefundRepository extends JpaRepository<Refund, UUID> {

    Optional<Refund> findByRefundNumber(String refundNumber);

    Optional<Refund> findByOrderId(UUID orderId);

    Page<Refund> findByStatus(RefundStatus status, Pageable pageable);

    Page<Refund> findByCustomerId(UUID customerId, Pageable pageable);

    Page<Refund> findBySellerId(UUID sellerId, Pageable pageable);

    @Query("SELECT r FROM Refund r WHERE " +
            "(:status IS NULL OR r.status = :status) AND " +
            "(:customerId IS NULL OR r.customerId = :customerId) AND " +
            "(:sellerId IS NULL OR r.sellerId = :sellerId) AND " +
            "(:dateFrom IS NULL OR r.createdAt >= :dateFrom) AND " +
            "(:dateTo IS NULL OR r.createdAt <= :dateTo) AND " +
            "(:query IS NULL OR r.refundNumber LIKE %:query% OR r.order.orderNumber LIKE %:query%)")
    Page<Refund> searchRefunds(
            @Param("status") RefundStatus status,
            @Param("customerId") UUID customerId,
            @Param("sellerId") UUID sellerId,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            @Param("query") String query,
            Pageable pageable);

    @Query("SELECT COUNT(r) FROM Refund r WHERE r.status = :status")
    long countByStatus(@Param("status") RefundStatus status);

    @Query("SELECT COUNT(r) FROM Refund r")
    long countAll();

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.status IN :statuses")
    BigDecimal sumAmountByStatus(@Param("statuses") List<RefundStatus> statuses);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") RefundStatus status);

    @Query("SELECT r.status, COUNT(r) FROM Refund r GROUP BY r.status")
    List<Object[]> countGroupByStatus();

    @Query("SELECT r.sellerId, COALESCE(SUM(r.amount), 0) FROM Refund r WHERE r.sellerId IS NOT NULL AND r.status IN :statuses GROUP BY r.sellerId")
    List<Object[]> sumAmountBySellerId(@Param("statuses") List<RefundStatus> statuses);
}

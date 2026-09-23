package ecommerce.modules.refund.repository;

import ecommerce.modules.refund.entity.Return;
import ecommerce.modules.refund.enums.ReturnStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReturnRepository extends JpaRepository<Return, Long> {

    Optional<Return> findByPublicId(UUID publicId);

    Optional<Return> findByReturnNumber(String returnNumber);

    List<Return> findByOrderId(UUID orderId);

    Page<Return> findByCustomerId(UUID customerId, Pageable pageable);

    Page<Return> findBySellerId(UUID sellerId, Pageable pageable);

    Page<Return> findByStatus(ReturnStatus status, Pageable pageable);

    @Query("SELECT r FROM Return r WHERE r.orderId = :orderId AND r.status NOT IN ('CANCELLED','REJECTED','EXPIRED')")
    List<Return> findActiveByOrderId(@Param("orderId") UUID orderId);

    boolean existsByOrderIdAndStatusNotIn(UUID orderId, List<ReturnStatus> statuses);
}

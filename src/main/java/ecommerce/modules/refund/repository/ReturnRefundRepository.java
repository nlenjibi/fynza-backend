package ecommerce.modules.refund.repository;

import ecommerce.modules.refund.entity.ReturnRefund;
import ecommerce.modules.refund.enums.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReturnRefundRepository extends JpaRepository<ReturnRefund, Long> {

    Optional<ReturnRefund> findByReturnId(UUID returnId);

    Optional<ReturnRefund> findByPublicId(UUID publicId);

    Page<ReturnRefund> findByStatus(RefundStatus status, Pageable pageable);
}

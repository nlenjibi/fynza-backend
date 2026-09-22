package ecommerce.modules.payout.repository;

import ecommerce.modules.payout.entity.Payout;
import ecommerce.modules.payout.enums.PayoutStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayoutRepository extends JpaRepository<Payout, Long>, JpaSpecificationExecutor<Payout> {

    Optional<Payout> findByPublicId(UUID publicId);

    Optional<Payout> findByIdempotencyKey(String idempotencyKey);

    Optional<Payout> findByProviderReference(String providerReference);

    Page<Payout> findBySellerIdOrderByRequestedAtDesc(Long sellerId, Pageable pageable);

    @Query("SELECT p FROM Payout p WHERE p.status IN :statuses AND p.retryCount < :maxRetries ORDER BY p.requestedAt ASC")
    List<Payout> findRetryablePayouts(@Param("statuses") List<PayoutStatus> statuses,
                                      @Param("maxRetries") int maxRetries,
                                      Pageable pageable);

    @Query("SELECT p FROM Payout p WHERE p.status = 'APPROVED' ORDER BY p.approvedAt ASC")
    List<Payout> findApprovedPayoutsForProcessing(Pageable pageable);

    @Query(value = "SELECT nextval('payout_number_seq')", nativeQuery = true)
    Long nextPayoutSequenceValue();
}

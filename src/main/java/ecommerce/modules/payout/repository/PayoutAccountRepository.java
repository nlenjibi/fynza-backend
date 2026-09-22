package ecommerce.modules.payout.repository;

import ecommerce.modules.payout.entity.PayoutAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayoutAccountRepository extends JpaRepository<PayoutAccount, Long>, JpaSpecificationExecutor<PayoutAccount> {

    Optional<PayoutAccount> findByPublicId(UUID publicId);

    List<PayoutAccount> findBySellerIdAndIsActiveTrue(Long sellerId);

    Optional<PayoutAccount> findBySellerIdAndIsDefaultTrueAndIsActiveTrue(Long sellerId);
}

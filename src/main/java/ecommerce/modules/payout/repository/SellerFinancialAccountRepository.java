package ecommerce.modules.payout.repository;

import ecommerce.modules.payout.entity.SellerFinancialAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SellerFinancialAccountRepository extends JpaRepository<SellerFinancialAccount, Long> {

    Optional<SellerFinancialAccount> findByPublicId(UUID publicId);

    Optional<SellerFinancialAccount> findBySellerId(Long sellerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM SellerFinancialAccount a WHERE a.sellerId = :sellerId")
    Optional<SellerFinancialAccount> findBySellerIdForUpdate(@Param("sellerId") Long sellerId);
}

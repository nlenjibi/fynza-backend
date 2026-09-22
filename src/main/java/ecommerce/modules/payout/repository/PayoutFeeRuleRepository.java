package ecommerce.modules.payout.repository;

import ecommerce.modules.payout.entity.PayoutFeeRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PayoutFeeRuleRepository extends JpaRepository<PayoutFeeRule, Long> {

    Optional<PayoutFeeRule> findByProviderAndPayoutTypeAndCurrencyAndIsActiveTrue(
            String provider, String payoutType, String currency);
}

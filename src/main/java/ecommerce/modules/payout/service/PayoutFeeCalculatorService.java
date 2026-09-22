package ecommerce.modules.payout.service;

import ecommerce.modules.payout.entity.PayoutFeeRule;
import ecommerce.modules.payout.repository.PayoutFeeRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutFeeCalculatorService {

    private final PayoutFeeRuleRepository feeRuleRepository;

    public BigDecimal calculateFee(String provider, String payoutType, String currency, BigDecimal amount) {
        Optional<PayoutFeeRule> ruleOpt = feeRuleRepository
                .findByProviderAndPayoutTypeAndCurrencyAndIsActiveTrue(provider, payoutType, currency);

        if (ruleOpt.isEmpty()) {
            log.debug("No fee rule found for provider={} payoutType={} currency={} — fee is ZERO",
                    provider, payoutType, currency);
            return BigDecimal.ZERO;
        }

        PayoutFeeRule rule = ruleOpt.get();
        BigDecimal fee = rule.getFlatFee().add(amount.multiply(rule.getPercentageFee()));

        // Apply min fee
        if (rule.getMinFee() != null && fee.compareTo(rule.getMinFee()) < 0) {
            fee = rule.getMinFee();
        }

        // Apply max fee
        if (rule.getMaxFee() != null && fee.compareTo(rule.getMaxFee()) > 0) {
            fee = rule.getMaxFee();
        }

        log.debug("Calculated fee={} for provider={} payoutType={} amount={}", fee, provider, payoutType, amount);
        return fee;
    }
}

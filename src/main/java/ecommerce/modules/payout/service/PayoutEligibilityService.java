package ecommerce.modules.payout.service;

import ecommerce.common.enums.SellerStatus;
import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.payout.entity.SellerFinancialAccount;
import ecommerce.modules.payout.exception.InsufficientBalanceException;
import ecommerce.modules.payout.exception.PayoutEligibilityException;
import ecommerce.modules.payout.provider.PayoutProperties;
import ecommerce.modules.payout.repository.SellerFinancialAccountRepository;
import ecommerce.modules.seller.entity.Seller;
import ecommerce.modules.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutEligibilityService {

    private final SellerRepository sellerRepository;
    private final SellerFinancialAccountRepository financialAccountRepository;
    private final PayoutProperties payoutProperties;

    public void checkEligibility(Long sellerId, BigDecimal requestedAmount) {
        // Check seller status
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found: " + sellerId));

        if (seller.getStatus() != SellerStatus.ACTIVE) {
            throw new PayoutEligibilityException(
                    "Seller account is not active. Current status: " + seller.getStatus());
        }

        // Check minimum payout amount
        BigDecimal minimum = payoutProperties.getMinimumAmount();
        if (requestedAmount.compareTo(minimum) < 0) {
            throw new PayoutEligibilityException(
                    "Requested amount " + requestedAmount + " is below minimum payout amount of " + minimum);
        }

        // Check available balance
        SellerFinancialAccount account = financialAccountRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new PayoutEligibilityException("No financial account found. Please contact support."));

        if (account.getAvailableBalance().compareTo(requestedAmount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient available balance. Available: " + account.getAvailableBalance()
                    + ", Requested: " + requestedAmount);
        }

        log.debug("Eligibility check passed sellerId={} requestedAmount={}", sellerId, requestedAmount);
    }
}

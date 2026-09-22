package ecommerce.modules.payout.service.impl;

import ecommerce.common.event.FynzaEventPublisher;
import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.payout.dto.response.PayoutResponse;
import ecommerce.modules.payout.entity.LedgerEntry;
import ecommerce.modules.payout.entity.Payout;
import ecommerce.modules.payout.entity.PayoutAccount;
import ecommerce.modules.payout.entity.SellerFinancialAccount;
import ecommerce.modules.payout.enums.LedgerDirection;
import ecommerce.modules.payout.enums.LedgerEntryType;
import ecommerce.modules.payout.enums.PayoutStatus;
import ecommerce.modules.payout.event.PayoutCancelledEvent;
import ecommerce.modules.payout.event.PayoutRequestedEvent;
import ecommerce.modules.payout.exception.PayoutAccountException;
import ecommerce.modules.payout.exception.PayoutEligibilityException;
import ecommerce.modules.payout.provider.PayoutProperties;
import ecommerce.modules.payout.repository.LedgerEntryRepository;
import ecommerce.modules.payout.repository.PayoutAccountRepository;
import ecommerce.modules.payout.repository.PayoutRepository;
import ecommerce.modules.payout.repository.SellerFinancialAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ecommerce.modules.payout.service.PayoutEligibilityService;
import ecommerce.modules.payout.service.PayoutFeeCalculatorService;
import ecommerce.modules.payout.service.PayoutService;
import ecommerce.modules.payout.service.SellerWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Year;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayoutServiceImpl implements PayoutService {

    private final PayoutRepository payoutRepository;
    private final PayoutAccountRepository payoutAccountRepository;
    private final SellerFinancialAccountRepository financialAccountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final SellerWalletService walletService;
    private final PayoutEligibilityService eligibilityService;
    private final PayoutFeeCalculatorService feeCalculatorService;
    private final FynzaEventPublisher eventPublisher;
    private final PayoutProperties payoutProperties;

    @Override
    @Transactional
    public PayoutResponse requestPayout(Long sellerId, UUID payoutAccountPublicId, BigDecimal amount, String idempotencyKey) {
        // 1. Idempotency check
        String effectiveKey = idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString();
        var existingOpt = payoutRepository.findByIdempotencyKey(effectiveKey);
        if (existingOpt.isPresent()) {
            log.info("Duplicate payout request — returning existing payout key={}", sanitize(effectiveKey));
            return toResponse(existingOpt.get());
        }

        // 2. Eligibility check
        eligibilityService.checkEligibility(sellerId, amount);

        // 3. Resolve payout account
        PayoutAccount payoutAccount = payoutAccountRepository.findByPublicId(payoutAccountPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Payout account not found: " + payoutAccountPublicId));
        if (!payoutAccount.getSellerId().equals(sellerId) || !Boolean.TRUE.equals(payoutAccount.getIsActive())) {
            throw new PayoutAccountException("Payout account does not belong to seller or is inactive");
        }
        if (!Boolean.TRUE.equals(payoutAccount.getIsVerified())) {
            throw new PayoutAccountException("Payout account is not verified");
        }

        // 4. Ensure financial account exists before locking
        walletService.getOrCreateWallet(sellerId);

        // 5. Calculate fee and net amount
        String provider = payoutProperties.getProvider();
        BigDecimal fee = feeCalculatorService.calculateFee(
                provider, payoutAccount.getType().name(), payoutAccount.getCurrency(), amount);
        BigDecimal netAmount = amount.subtract(fee);

        // 6. Lock financial account and reserve balance
        SellerFinancialAccount lockedAccount = financialAccountRepository
                .findBySellerIdForUpdate(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Financial account not found for seller: " + sellerId));

        if (lockedAccount.getAvailableBalance().compareTo(amount) < 0) {
            throw new PayoutEligibilityException("Insufficient available balance at time of reservation");
        }

        BigDecimal availableBefore = lockedAccount.getAvailableBalance();
        BigDecimal reservedBefore = lockedAccount.getReservedBalance();

        lockedAccount.setAvailableBalance(availableBefore.subtract(amount));
        lockedAccount.setReservedBalance(reservedBefore.add(amount));
        financialAccountRepository.save(lockedAccount);

        // 7. Write reservation ledger entry
        writeLedgerEntry(
                lockedAccount.getId(), LedgerEntryType.PAYOUT_RESERVATION, LedgerDirection.DEBIT,
                "PAYOUT_REQUEST", null, amount, payoutAccount.getCurrency(),
                availableBefore, lockedAccount.getAvailableBalance(),
                "Balance reserved for payout request");

        // 8. Generate payout number
        String payoutNumber = generatePayoutNumber();

        // 9. Create payout record
        Payout payout = Payout.builder()
                .sellerId(sellerId)
                .payoutAccountId(payoutAccount.getId())
                .financialAccountId(lockedAccount.getId())
                .payoutNumber(payoutNumber)
                .amount(amount)
                .fee(fee)
                .netAmount(netAmount)
                .currency(payoutAccount.getCurrency())
                .status(PayoutStatus.REQUESTED)
                .idempotencyKey(effectiveKey)
                .retryCount(0)
                .build();

        Payout saved = payoutRepository.save(payout);
        log.info("Payout requested sellerId={} payoutNumber={} amount={}", sellerId, payoutNumber, amount);

        // 10. Publish domain event
        eventPublisher.publish(new PayoutRequestedEvent(
                saved.getId(), saved.getPublicId(), sellerId, payoutNumber, amount, payoutAccount.getCurrency()));

        return toResponse(saved);
    }

    @Override
    @Transactional
    public PayoutResponse cancelPayout(Long sellerId, UUID payoutPublicId) {
        Payout payout = payoutRepository.findByPublicId(payoutPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Payout not found: " + payoutPublicId));

        if (!payout.getSellerId().equals(sellerId)) {
            throw new PayoutEligibilityException("Payout does not belong to seller");
        }

        if (payout.getStatus() != PayoutStatus.REQUESTED && payout.getStatus() != PayoutStatus.PENDING_APPROVAL) {
            throw new PayoutEligibilityException(
                    "Payout cannot be cancelled in status: " + payout.getStatus());
        }

        // Release reserved balance back to available
        SellerFinancialAccount lockedAccount = financialAccountRepository
                .findBySellerIdForUpdate(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Financial account not found"));

        BigDecimal reservedBefore = lockedAccount.getReservedBalance();
        BigDecimal availableBefore = lockedAccount.getAvailableBalance();

        lockedAccount.setReservedBalance(reservedBefore.subtract(payout.getAmount()));
        lockedAccount.setAvailableBalance(availableBefore.add(payout.getAmount()));
        financialAccountRepository.save(lockedAccount);

        // Write release ledger entry
        writeLedgerEntry(
                lockedAccount.getId(), LedgerEntryType.PAYOUT_RELEASE, LedgerDirection.CREDIT,
                "PAYOUT", payout.getPublicId(), payout.getAmount(), payout.getCurrency(),
                reservedBefore, lockedAccount.getReservedBalance(),
                "Payout cancelled — balance released");

        payout.setStatus(PayoutStatus.CANCELLED);
        Payout saved = payoutRepository.save(payout);
        log.info("Payout cancelled sellerId={} payoutId={}", sellerId, payoutPublicId);

        eventPublisher.publish(new PayoutCancelledEvent(
                saved.getId(), saved.getPublicId(), sellerId, saved.getPayoutNumber(),
                saved.getAmount(), saved.getCurrency(), "Cancelled by seller"));

        return toResponse(saved);
    }

    @Override
    public Page<PayoutResponse> listPayouts(Long sellerId, Pageable pageable) {
        return payoutRepository.findBySellerIdOrderByRequestedAtDesc(sellerId, pageable)
                .map(this::toResponse);
    }

    @Override
    public Optional<PayoutResponse> findPayout(Long sellerId, UUID payoutPublicId) {
        return payoutRepository.findByPublicId(payoutPublicId)
                .filter(p -> p.getSellerId().equals(sellerId))
                .map(this::toResponse);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String generatePayoutNumber() {
        long seq = payoutRepository.nextPayoutSequenceValue();
        return "PAY-" + Year.now().getValue() + "-" + String.format("%06d", seq);
    }

    private LedgerEntry writeLedgerEntry(Long accountId, LedgerEntryType type, LedgerDirection direction,
                                         String refType, UUID refId, BigDecimal amount, String currency,
                                         BigDecimal before, BigDecimal after, String description) {
        return ledgerEntryRepository.save(LedgerEntry.builder()
                .financialAccountId(accountId)
                .entryType(type)
                .direction(direction)
                .referenceType(refType)
                .referenceId(refId)
                .amount(amount)
                .currency(currency)
                .balanceBefore(before)
                .balanceAfter(after)
                .description(description)
                .build());
    }

    private PayoutResponse toResponse(Payout payout) {
        return PayoutResponse.builder()
                .id(payout.getPublicId())
                .payoutNumber(payout.getPayoutNumber())
                .amount(payout.getAmount())
                .fee(payout.getFee())
                .netAmount(payout.getNetAmount())
                .currency(payout.getCurrency())
                .status(payout.getStatus())
                .providerReference(payout.getProviderReference())
                .failureReason(payout.getFailureReason())
                .retryCount(payout.getRetryCount())
                .requestedAt(payout.getRequestedAt())
                .approvedAt(payout.getApprovedAt())
                .completedAt(payout.getCompletedAt())
                .build();
    }

    private static String sanitize(String value) {
        if (value == null) return "";
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            sb.append(Character.isISOControl(c) ? '_' : c);
        }
        return sb.toString();
    }
}

package ecommerce.modules.payout.service.impl;

import ecommerce.common.event.FynzaEventPublisher;
import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.payout.dto.response.PayoutResponse;
import ecommerce.modules.payout.entity.LedgerEntry;
import ecommerce.modules.payout.entity.Payout;
import ecommerce.modules.payout.entity.SellerFinancialAccount;
import ecommerce.modules.payout.enums.LedgerDirection;
import ecommerce.modules.payout.enums.LedgerEntryType;
import ecommerce.modules.payout.enums.PayoutStatus;
import ecommerce.modules.payout.event.PayoutApprovedEvent;
import ecommerce.modules.payout.event.PayoutCancelledEvent;
import ecommerce.modules.payout.event.PayoutHeldEvent;
import ecommerce.modules.payout.exception.PayoutEligibilityException;
import ecommerce.modules.payout.provider.PayoutProperties;
import ecommerce.modules.payout.repository.LedgerEntryRepository;
import ecommerce.modules.payout.repository.PayoutRepository;
import ecommerce.modules.payout.repository.SellerFinancialAccountRepository;
import ecommerce.modules.payout.service.PayoutAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayoutAdminServiceImpl implements PayoutAdminService {

    private final PayoutRepository payoutRepository;
    private final SellerFinancialAccountRepository financialAccountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final FynzaEventPublisher eventPublisher;
    private final PayoutProperties payoutProperties;

    @Override
    @Transactional
    public PayoutResponse approvePayout(UUID payoutPublicId) {
        Payout payout = requirePayout(payoutPublicId);

        if (payout.getStatus() != PayoutStatus.REQUESTED && payout.getStatus() != PayoutStatus.PENDING_APPROVAL) {
            throw new PayoutEligibilityException("Payout cannot be approved from status: " + payout.getStatus());
        }

        payout.setStatus(PayoutStatus.APPROVED);
        payout.setApprovedAt(Instant.now());
        Payout saved = payoutRepository.save(payout);
        log.info("Payout approved payoutId={}", payoutPublicId);

        eventPublisher.publish(new PayoutApprovedEvent(
                saved.getId(), saved.getPublicId(), saved.getSellerId(),
                saved.getPayoutNumber(), saved.getAmount(), saved.getCurrency()));

        return toResponse(saved);
    }

    @Override
    @Transactional
    public PayoutResponse rejectPayout(UUID payoutPublicId, String reason) {
        Payout payout = requirePayout(payoutPublicId);

        if (payout.getStatus() != PayoutStatus.REQUESTED && payout.getStatus() != PayoutStatus.PENDING_APPROVAL) {
            throw new PayoutEligibilityException("Payout cannot be rejected from status: " + payout.getStatus());
        }

        // Release reserved balance
        releaseReservedBalance(payout, reason);

        payout.setStatus(PayoutStatus.CANCELLED);
        payout.setFailureReason(reason);
        Payout saved = payoutRepository.save(payout);
        log.info("Payout rejected payoutId={} reason={}", payoutPublicId, sanitize(reason));

        eventPublisher.publish(new PayoutCancelledEvent(
                saved.getId(), saved.getPublicId(), saved.getSellerId(),
                saved.getPayoutNumber(), saved.getAmount(), saved.getCurrency(), reason));

        return toResponse(saved);
    }

    @Override
    @Transactional
    public PayoutResponse holdPayout(UUID payoutPublicId, String reason) {
        Payout payout = requirePayout(payoutPublicId);

        if (payout.getStatus() != PayoutStatus.REQUESTED && payout.getStatus() != PayoutStatus.APPROVED) {
            throw new PayoutEligibilityException("Payout cannot be placed on hold from status: " + payout.getStatus());
        }

        payout.setStatus(PayoutStatus.ON_HOLD);
        payout.setFailureReason(reason);
        Payout saved = payoutRepository.save(payout);
        log.info("Payout placed on hold payoutId={}", payoutPublicId);

        eventPublisher.publish(new PayoutHeldEvent(
                saved.getId(), saved.getPublicId(), saved.getSellerId(),
                saved.getPayoutNumber(), saved.getAmount(), saved.getCurrency(), reason));

        return toResponse(saved);
    }

    @Override
    @Transactional
    public PayoutResponse retryPayout(UUID payoutPublicId) {
        Payout payout = requirePayout(payoutPublicId);

        if (payout.getStatus() != PayoutStatus.FAILED) {
            throw new PayoutEligibilityException("Only FAILED payouts can be retried");
        }

        int maxRetries = payoutProperties.getMaxRetryCount();
        if (payout.getRetryCount() >= maxRetries) {
            throw new PayoutEligibilityException(
                    "Maximum retry count (" + maxRetries + ") exceeded for payout: " + payoutPublicId);
        }

        payout.setStatus(PayoutStatus.APPROVED);
        payout.setFailureReason(null);
        payout.setApprovedAt(Instant.now());
        Payout saved = payoutRepository.save(payout);
        log.info("Payout queued for retry payoutId={} retryCount={}", payoutPublicId, payout.getRetryCount());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public PayoutResponse cancelPayout(UUID payoutPublicId) {
        Payout payout = requirePayout(payoutPublicId);

        if (payout.getStatus() == PayoutStatus.COMPLETED || payout.getStatus() == PayoutStatus.CANCELLED) {
            throw new PayoutEligibilityException("Payout cannot be cancelled from status: " + payout.getStatus());
        }

        // Release reserved/processing balance if applicable
        if (payout.getStatus() != PayoutStatus.PROCESSING) {
            releaseReservedBalance(payout, "Admin cancelled");
        }

        payout.setStatus(PayoutStatus.CANCELLED);
        Payout saved = payoutRepository.save(payout);
        log.info("Payout cancelled by admin payoutId={}", payoutPublicId);

        eventPublisher.publish(new PayoutCancelledEvent(
                saved.getId(), saved.getPublicId(), saved.getSellerId(),
                saved.getPayoutNumber(), saved.getAmount(), saved.getCurrency(), "Admin cancelled"));

        return toResponse(saved);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Payout requirePayout(UUID payoutPublicId) {
        return payoutRepository.findByPublicId(payoutPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Payout not found: " + payoutPublicId));
    }

    private void releaseReservedBalance(Payout payout, String reason) {
        SellerFinancialAccount account = financialAccountRepository
                .findBySellerIdForUpdate(payout.getSellerId())
                .orElse(null);

        if (account == null) {
            log.warn("Could not find financial account to release balance for payout={}",
                    payout.getPublicId());
            return;
        }

        BigDecimal reservedBefore = account.getReservedBalance();
        BigDecimal availableBefore = account.getAvailableBalance();

        account.setReservedBalance(reservedBefore.subtract(payout.getAmount()));
        account.setAvailableBalance(availableBefore.add(payout.getAmount()));
        financialAccountRepository.save(account);

        writeLedgerEntry(account.getId(), LedgerEntryType.PAYOUT_RELEASE, LedgerDirection.CREDIT,
                "PAYOUT", payout.getPublicId(), payout.getAmount(), payout.getCurrency(),
                reservedBefore, account.getReservedBalance(),
                "Payout rejected/cancelled — balance released: " + reason);
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
        return value == null ? "" : value.replaceAll("[\\r\\n\\t]", "_");
    }
}

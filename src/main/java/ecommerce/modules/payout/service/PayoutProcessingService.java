package ecommerce.modules.payout.service;

import ecommerce.common.event.FynzaEventPublisher;
import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.payout.entity.LedgerEntry;
import ecommerce.modules.payout.entity.Payout;
import ecommerce.modules.payout.entity.SellerFinancialAccount;
import ecommerce.modules.payout.enums.LedgerDirection;
import ecommerce.modules.payout.enums.LedgerEntryType;
import ecommerce.modules.payout.enums.PayoutStatus;
import ecommerce.modules.payout.event.PayoutCompletedEvent;
import ecommerce.modules.payout.event.PayoutFailedEvent;
import ecommerce.modules.payout.provider.PayoutProperties;
import ecommerce.modules.payout.provider.PayoutProvider;
import ecommerce.modules.payout.provider.dto.PayoutDispatchRequest;
import ecommerce.modules.payout.provider.dto.PayoutDispatchResult;
import ecommerce.modules.payout.repository.LedgerEntryRepository;
import ecommerce.modules.payout.repository.PayoutAccountRepository;
import ecommerce.modules.payout.repository.PayoutRepository;
import ecommerce.modules.payout.repository.SellerFinancialAccountRepository;
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
public class PayoutProcessingService {

    private final PayoutRepository payoutRepository;
    private final PayoutAccountRepository payoutAccountRepository;
    private final SellerFinancialAccountRepository financialAccountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final PayoutProvider payoutProvider;
    private final FynzaEventPublisher eventPublisher;
    private final PayoutProperties payoutProperties;

    @Transactional
    public void processPayout(Long payoutId) {
        Payout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new ResourceNotFoundException("Payout not found: " + payoutId));

        if (payout.getStatus() != PayoutStatus.APPROVED) {
            log.warn("Skipping non-APPROVED payout id={} status={}", payoutId, payout.getStatus());
            return;
        }

        // Mark as processing
        payout.setStatus(PayoutStatus.PROCESSING);
        payout.setProcessedAt(Instant.now());
        payoutRepository.save(payout);

        try {
            // Resolve payout account for dispatch details
            var payoutAccount = payoutAccountRepository.findById(payout.getPayoutAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Payout account not found"));

            SellerFinancialAccount financialAccount = financialAccountRepository.findById(payout.getFinancialAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Financial account not found"));

            PayoutDispatchRequest dispatchRequest = PayoutDispatchRequest.builder()
                    .idempotencyKey(payout.getIdempotencyKey())
                    .accountIdentifier(payoutAccount.getAccountIdentifier())
                    .bankCode(payoutAccount.getBankCode())
                    .accountName(payoutAccount.getAccountName())
                    .amount(payout.getNetAmount())
                    .currency(payout.getCurrency())
                    .narration("Fynza seller payout " + payout.getPayoutNumber())
                    .payoutPublicId(payout.getPublicId())
                    .build();

            PayoutDispatchResult result = payoutProvider.dispatch(dispatchRequest);

            if (result.isSuccess()) {
                handleSuccess(payout, financialAccount, result.getProviderReference());
            } else {
                handleFailure(payout, financialAccount, result.getMessage());
            }

        } catch (Exception e) {
            log.error("Error processing payout id={}: {}", payoutId, e.getMessage(), e);
            SellerFinancialAccount financialAccount = financialAccountRepository
                    .findById(payout.getFinancialAccountId()).orElse(null);
            if (financialAccount != null) {
                handleFailure(payout, financialAccount, e.getMessage());
            } else {
                payout.setStatus(PayoutStatus.FAILED);
                payout.setFailureReason(e.getMessage());
                payout.setRetryCount(payout.getRetryCount() + 1);
                payoutRepository.save(payout);
            }
        }
    }

    private void handleSuccess(Payout payout, SellerFinancialAccount account, String providerReference) {
        SellerFinancialAccount lockedAccount = financialAccountRepository
                .findBySellerIdForUpdate(account.getSellerId()).orElse(account);

        BigDecimal reservedBefore = lockedAccount.getReservedBalance();
        BigDecimal totalPaidBefore = lockedAccount.getTotalPaidOut();

        lockedAccount.setReservedBalance(reservedBefore.subtract(payout.getAmount()));
        lockedAccount.setTotalPaidOut(totalPaidBefore.add(payout.getNetAmount()));
        financialAccountRepository.save(lockedAccount);

        // Write completed ledger entry
        writeLedgerEntry(lockedAccount.getId(), LedgerEntryType.PAYOUT_COMPLETED, LedgerDirection.DEBIT,
                "PAYOUT", payout.getPublicId(), payout.getAmount(), payout.getCurrency(),
                reservedBefore, lockedAccount.getReservedBalance(), "Payout completed successfully");

        // Write fee ledger entry if fee > 0
        if (payout.getFee().compareTo(BigDecimal.ZERO) > 0) {
            writeLedgerEntry(lockedAccount.getId(), LedgerEntryType.PAYOUT_FEE, LedgerDirection.DEBIT,
                    "PAYOUT", payout.getPublicId(), payout.getFee(), payout.getCurrency(),
                    lockedAccount.getReservedBalance(), lockedAccount.getReservedBalance(),
                    "Payout fee");
        }

        payout.setStatus(PayoutStatus.COMPLETED);
        payout.setProviderReference(providerReference);
        payout.setCompletedAt(Instant.now());
        payoutRepository.save(payout);

        log.info("Payout completed id={} payoutNumber={} providerRef={}",
                payout.getId(), payout.getPayoutNumber(), sanitize(providerReference));

        eventPublisher.publish(new PayoutCompletedEvent(
                payout.getId(), payout.getPublicId(), payout.getSellerId(),
                payout.getPayoutNumber(), payout.getAmount(), payout.getCurrency()));
    }

    private void handleFailure(Payout payout, SellerFinancialAccount account, String reason) {
        SellerFinancialAccount lockedAccount = financialAccountRepository
                .findBySellerIdForUpdate(account.getSellerId()).orElse(account);

        // Restore reserved balance to available
        BigDecimal reservedBefore = lockedAccount.getReservedBalance();
        BigDecimal availableBefore = lockedAccount.getAvailableBalance();

        lockedAccount.setReservedBalance(reservedBefore.subtract(payout.getAmount()));
        lockedAccount.setAvailableBalance(availableBefore.add(payout.getAmount()));
        financialAccountRepository.save(lockedAccount);

        // Write release ledger entry
        writeLedgerEntry(lockedAccount.getId(), LedgerEntryType.PAYOUT_RELEASE, LedgerDirection.CREDIT,
                "PAYOUT", payout.getPublicId(), payout.getAmount(), payout.getCurrency(),
                reservedBefore, lockedAccount.getReservedBalance(),
                "Payout failed — balance released: " + reason);

        payout.setStatus(PayoutStatus.FAILED);
        payout.setFailureReason(reason);
        payout.setRetryCount(payout.getRetryCount() + 1);
        payoutRepository.save(payout);

        log.warn("Payout failed id={} payoutNumber={} reason={}",
                payout.getId(), payout.getPayoutNumber(), sanitize(reason));

        eventPublisher.publish(new PayoutFailedEvent(
                payout.getId(), payout.getPublicId(), payout.getSellerId(),
                payout.getPayoutNumber(), payout.getAmount(), payout.getCurrency(), reason));
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

    private static String sanitize(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n\\t]", "_");
    }
}

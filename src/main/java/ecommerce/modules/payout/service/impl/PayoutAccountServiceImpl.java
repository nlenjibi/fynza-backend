package ecommerce.modules.payout.service.impl;

import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.payout.dto.request.AddPayoutAccountRequest;
import ecommerce.modules.payout.dto.request.UpdatePayoutAccountRequest;
import ecommerce.modules.payout.dto.response.PayoutAccountResponse;
import ecommerce.modules.payout.entity.PayoutAccount;
import ecommerce.modules.payout.enums.PayoutAccountStatus;
import ecommerce.modules.payout.exception.PayoutAccountException;
import ecommerce.modules.payout.repository.PayoutAccountRepository;
import ecommerce.modules.payout.service.PayoutAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayoutAccountServiceImpl implements PayoutAccountService {

    private final PayoutAccountRepository payoutAccountRepository;

    @Override
    @Transactional
    public PayoutAccountResponse addAccount(Long sellerId, AddPayoutAccountRequest request) {
        // If isDefault, un-default all existing accounts
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            unDefaultAll(sellerId);
        }

        // Mask account number — store only last 4 digits visible
        String maskedNumber = maskAccountNumber(request.getAccountNumber());

        PayoutAccount account = PayoutAccount.builder()
                .sellerId(sellerId)
                .type(request.getType())
                .provider(request.getProvider())
                .accountName(request.getAccountName())
                .accountNumber(maskedNumber)
                .accountIdentifier(request.getAccountNumber())
                .bankCode(request.getBankCode())
                .country(request.getCountry())
                .currency(request.getCurrency() != null ? request.getCurrency() : "GHS")
                .isDefault(Boolean.TRUE.equals(request.getIsDefault()))
                .isVerified(false)
                .status(PayoutAccountStatus.PENDING)
                .isActive(true)
                .build();

        PayoutAccount saved = payoutAccountRepository.save(account);
        log.info("Payout account added sellerId={} accountId={}", sellerId, saved.getPublicId());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PayoutAccountResponse updateAccount(Long sellerId, UUID accountPublicId, UpdatePayoutAccountRequest request) {
        PayoutAccount account = findActiveAccountForSeller(sellerId, accountPublicId);

        if (request.getAccountName() != null) {
            account.setAccountName(request.getAccountName());
        }
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            unDefaultAll(sellerId);
            account.setIsDefault(true);
        } else if (Boolean.FALSE.equals(request.getIsDefault())) {
            account.setIsDefault(false);
        }

        PayoutAccount saved = payoutAccountRepository.save(account);
        log.info("Payout account updated sellerId={} accountId={}", sellerId, accountPublicId);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void removeAccount(Long sellerId, UUID accountPublicId) {
        PayoutAccount account = findActiveAccountForSeller(sellerId, accountPublicId);
        account.setIsActive(false);
        account.setStatus(PayoutAccountStatus.REMOVED);
        if (Boolean.TRUE.equals(account.getIsDefault())) {
            account.setIsDefault(false);
        }
        payoutAccountRepository.save(account);
        log.info("Payout account soft-deleted sellerId={} accountId={}", sellerId, accountPublicId);
    }

    @Override
    @Transactional
    public PayoutAccountResponse setDefault(Long sellerId, UUID accountPublicId) {
        findActiveAccountForSeller(sellerId, accountPublicId); // ownership check
        unDefaultAll(sellerId);
        PayoutAccount account = findActiveAccountForSeller(sellerId, accountPublicId);
        account.setIsDefault(true);
        PayoutAccount saved = payoutAccountRepository.save(account);
        log.info("Default payout account set sellerId={} accountId={}", sellerId, accountPublicId);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PayoutAccountResponse verifyAccount(UUID accountPublicId) {
        PayoutAccount account = payoutAccountRepository.findByPublicId(accountPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Payout account not found: " + accountPublicId));
        account.setIsVerified(true);
        account.setVerifiedAt(Instant.now());
        account.setStatus(PayoutAccountStatus.ACTIVE);
        PayoutAccount saved = payoutAccountRepository.save(account);
        log.info("Payout account verified accountId={}", accountPublicId);
        return toResponse(saved);
    }

    @Override
    public List<PayoutAccountResponse> listAccounts(Long sellerId) {
        return payoutAccountRepository.findBySellerIdAndIsActiveTrue(sellerId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PayoutAccount findActiveAccountForSeller(Long sellerId, UUID accountPublicId) {
        PayoutAccount account = payoutAccountRepository.findByPublicId(accountPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Payout account not found: " + accountPublicId));
        if (!account.getSellerId().equals(sellerId) || !Boolean.TRUE.equals(account.getIsActive())) {
            throw new PayoutAccountException("Payout account does not belong to seller or is inactive");
        }
        return account;
    }

    private void unDefaultAll(Long sellerId) {
        List<PayoutAccount> accounts = payoutAccountRepository.findBySellerIdAndIsActiveTrue(sellerId);
        accounts.stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsDefault()))
                .forEach(a -> {
                    a.setIsDefault(false);
                    payoutAccountRepository.save(a);
                });
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****";
        }
        String last4 = accountNumber.substring(accountNumber.length() - 4);
        return "****" + last4;
    }

    private PayoutAccountResponse toResponse(PayoutAccount account) {
        return PayoutAccountResponse.builder()
                .id(account.getPublicId())
                .type(account.getType())
                .provider(account.getProvider())
                .accountName(account.getAccountName())
                .accountNumber(account.getAccountNumber())
                .country(account.getCountry())
                .currency(account.getCurrency())
                .isDefault(account.getIsDefault())
                .isVerified(account.getIsVerified())
                .status(account.getStatus())
                .verifiedAt(account.getVerifiedAt())
                .build();
    }
}

package ecommerce.modules.payout.service.impl;

import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.payout.dto.response.WalletResponse;
import ecommerce.modules.payout.entity.LedgerEntry;
import ecommerce.modules.payout.entity.SellerFinancialAccount;
import ecommerce.modules.payout.enums.LedgerDirection;
import ecommerce.modules.payout.enums.LedgerEntryType;
import ecommerce.modules.payout.exception.InsufficientBalanceException;
import ecommerce.modules.payout.repository.LedgerEntryRepository;
import ecommerce.modules.payout.repository.SellerFinancialAccountRepository;
import ecommerce.modules.payout.service.SellerWalletService;
import ecommerce.modules.seller.entity.Seller;
import ecommerce.modules.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerWalletServiceImpl implements SellerWalletService {

    private final SellerFinancialAccountRepository financialAccountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final SellerRepository sellerRepository;

    @Override
    public WalletResponse getWallet(UUID sellerPublicId) {
        Seller seller = sellerRepository.findByPublicId(sellerPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found: " + sellerPublicId));
        SellerFinancialAccount account = getOrCreateWallet(seller.getId());
        return toResponse(account);
    }

    @Override
    @Transactional
    public SellerFinancialAccount getOrCreateWallet(Long sellerId) {
        return financialAccountRepository.findBySellerId(sellerId)
                .orElseGet(() -> {
                    log.info("Creating new financial account for sellerId={}", sellerId);
                    SellerFinancialAccount account = SellerFinancialAccount.builder()
                            .sellerId(sellerId)
                            .pendingBalance(BigDecimal.ZERO)
                            .availableBalance(BigDecimal.ZERO)
                            .reservedBalance(BigDecimal.ZERO)
                            .totalEarned(BigDecimal.ZERO)
                            .totalPaidOut(BigDecimal.ZERO)
                            .build();
                    return financialAccountRepository.save(account);
                });
    }

    @Override
    @Transactional
    public void creditEarning(Long sellerId, BigDecimal amount, UUID orderId) {
        SellerFinancialAccount account = financialAccountRepository.findBySellerIdForUpdate(sellerId)
                .orElseGet(() -> getOrCreateWallet(sellerId));

        BigDecimal before = account.getPendingBalance();
        BigDecimal after = before.add(amount);
        account.setPendingBalance(after);
        account.setTotalEarned(account.getTotalEarned().add(amount));
        financialAccountRepository.save(account);

        ledgerEntryRepository.save(LedgerEntry.builder()
                .financialAccountId(account.getId())
                .entryType(LedgerEntryType.ORDER_EARNING)
                .direction(LedgerDirection.CREDIT)
                .referenceType("ORDER")
                .referenceId(orderId)
                .amount(amount)
                .currency("GHS")
                .balanceBefore(before)
                .balanceAfter(after)
                .description("Order earning credited")
                .build());

        log.info("Credited earning sellerId={} amount={} orderId={}", sellerId, amount, orderId);
    }

    @Override
    @Transactional
    public void settlePendingToAvailable(Long sellerId, BigDecimal amount) {
        SellerFinancialAccount account = financialAccountRepository.findBySellerIdForUpdate(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Financial account not found for seller: " + sellerId));

        if (account.getPendingBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient pending balance to settle");
        }

        account.setPendingBalance(account.getPendingBalance().subtract(amount));
        account.setAvailableBalance(account.getAvailableBalance().add(amount));
        financialAccountRepository.save(account);

        log.info("Settled pending to available sellerId={} amount={}", sellerId, amount);
    }

    private WalletResponse toResponse(SellerFinancialAccount account) {
        return WalletResponse.builder()
                .publicId(account.getPublicId())
                .pendingBalance(account.getPendingBalance())
                .availableBalance(account.getAvailableBalance())
                .reservedBalance(account.getReservedBalance())
                .totalEarned(account.getTotalEarned())
                .totalPaidOut(account.getTotalPaidOut())
                .currency("GHS")
                .build();
    }
}

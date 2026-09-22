package ecommerce.modules.payout.service;

import ecommerce.modules.payout.dto.response.WalletResponse;
import ecommerce.modules.payout.entity.SellerFinancialAccount;

import java.math.BigDecimal;
import java.util.UUID;

public interface SellerWalletService {

    WalletResponse getWallet(UUID sellerPublicId);

    SellerFinancialAccount getOrCreateWallet(Long sellerId);

    void creditEarning(Long sellerId, BigDecimal amount, UUID orderId);

    void settlePendingToAvailable(Long sellerId, BigDecimal amount);
}

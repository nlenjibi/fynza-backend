package ecommerce.graphql.resolver.payout;

import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.payout.dto.response.PayoutAccountResponse;
import ecommerce.modules.payout.dto.response.PayoutResponse;
import ecommerce.modules.payout.dto.response.WalletResponse;
import ecommerce.modules.payout.service.PayoutAccountService;
import ecommerce.modules.payout.service.PayoutService;
import ecommerce.modules.payout.service.SellerWalletService;
import ecommerce.modules.seller.entity.Seller;
import ecommerce.modules.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PayoutQueryResolver {

    private final SellerWalletService walletService;
    private final PayoutService payoutService;
    private final PayoutAccountService payoutAccountService;
    private final SellerRepository sellerRepository;

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public WalletResponse wallet(@AuthenticationPrincipal UserPrincipal principal) {
        UUID sellerPublicId = resolveSellerPublicId(principal);
        log.debug("GQL wallet userId={}", principal.getId());
        return walletService.getWallet(sellerPublicId);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Page<PayoutResponse> myPayouts(
            @Argument int page,
            @Argument int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long sellerId = resolveSellerId(principal);
        log.debug("GQL myPayouts userId={} page={} size={}", principal.getId(), page, size);
        return payoutService.listPayouts(sellerId, PageRequest.of(page, size));
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public PayoutResponse payout(
            @Argument UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long sellerId = resolveSellerId(principal);
        log.debug("GQL payout userId={} payoutId={}", principal.getId(), id);
        return payoutService.findPayout(sellerId, id).orElse(null);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<PayoutAccountResponse> myPayoutAccounts(@AuthenticationPrincipal UserPrincipal principal) {
        Long sellerId = resolveSellerId(principal);
        log.debug("GQL myPayoutAccounts userId={}", principal.getId());
        return payoutAccountService.listAccounts(sellerId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Long resolveSellerId(UserPrincipal principal) {
        Seller seller = sellerRepository.findByOwnerUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));
        return seller.getId();
    }

    private UUID resolveSellerPublicId(UserPrincipal principal) {
        Seller seller = sellerRepository.findByOwnerUserId(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));
        return seller.getPublicId();
    }
}

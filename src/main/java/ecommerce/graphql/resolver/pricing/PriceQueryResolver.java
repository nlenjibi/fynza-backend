package ecommerce.graphql.resolver.pricing;

import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.pricing.dto.response.PriceHistoryResponse;
import ecommerce.modules.pricing.dto.response.PriceResponse;
import ecommerce.modules.pricing.dto.response.PriceResultResponse;
import ecommerce.modules.pricing.enums.SupportedCurrency;
import ecommerce.modules.pricing.service.AdminPriceService;
import ecommerce.modules.pricing.service.PriceResolverService;
import ecommerce.modules.pricing.service.PriceService;
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
public class PriceQueryResolver {

    private final PriceResolverService resolverService;
    private final PriceService         priceService;
    private final AdminPriceService    adminPriceService;

    // ── Public ────────────────────────────────────────────────────────────────

    @QueryMapping
    public PriceResultResponse effectivePrice(
            @Argument String productId,
            @Argument String variantId,
            @Argument Integer quantity,
            @Argument String currency) {
        log.debug("GQL effectivePrice productId={} variantId={} qty={}", productId, variantId, quantity);
        SupportedCurrency cur = currency != null ? SupportedCurrency.valueOf(currency) : SupportedCurrency.GHS;
        int qty = quantity != null ? quantity : 1;
        return resolverService.resolve(
                UUID.fromString(productId),
                variantId != null ? UUID.fromString(variantId) : null,
                qty, cur);
    }

    @QueryMapping
    public PriceResultResponse price(
            @Argument String productId,
            @Argument String variantId,
            @Argument String currency) {
        log.debug("GQL price productId={}", productId);
        SupportedCurrency cur = currency != null ? SupportedCurrency.valueOf(currency) : SupportedCurrency.GHS;
        return resolverService.resolve(
                UUID.fromString(productId),
                variantId != null ? UUID.fromString(variantId) : null,
                1, cur);
    }

    @QueryMapping
    public List<PriceResponse> prices(
            @Argument String productId,
            @Argument String currency) {
        log.debug("GQL prices productId={}", productId);
        return adminPriceService.getPricesByProductId(UUID.fromString(productId));
    }

    // ── Seller ────────────────────────────────────────────────────────────────

    @QueryMapping
    @PreAuthorize("hasAuthority('price.read')")
    public Page<PriceResponse> sellerPrices(
            @Argument String productId,
            @Argument Integer page,
            @Argument Integer size,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL sellerPrices productId={} user={}", productId, principal.getId());
        return priceService.getSellerPrices(
                UUID.fromString(productId),
                principal.getId(),
                PageRequest.of(page != null ? page : 0, size != null ? size : 20));
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('price.read')")
    public Page<PriceResponse> adminPrices(
            @Argument Integer page,
            @Argument Integer size) {
        log.debug("GQL adminPrices page={}", page);
        return adminPriceService.getAllPrices(
                PageRequest.of(page != null ? page : 0, size != null ? size : 20));
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('price.read')")
    public PriceResponse adminPrice(@Argument String id) {
        log.debug("GQL adminPrice id={}", id);
        return adminPriceService.getPriceById(UUID.fromString(id));
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('price.history.read')")
    public List<PriceHistoryResponse> priceHistory(
            @Argument String priceId,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL priceHistory priceId={}", priceId);
        return adminPriceService.getPriceHistory(UUID.fromString(priceId));
    }
}

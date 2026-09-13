package ecommerce.modules.pricing.service.impl;

import ecommerce.modules.pricing.dto.response.PriceResultResponse;
import ecommerce.modules.pricing.entity.Price;
import ecommerce.modules.pricing.entity.PriceTier;
import ecommerce.modules.pricing.enums.SupportedCurrency;
import ecommerce.modules.pricing.exception.PriceNotFoundException;
import ecommerce.modules.pricing.repository.PriceListRepository;
import ecommerce.modules.pricing.repository.PriceRepository;
import ecommerce.modules.pricing.repository.PriceTierRepository;
import ecommerce.modules.pricing.service.PriceResolverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PriceResolverServiceImpl implements PriceResolverService {

    private final PriceRepository     priceRepository;
    private final PriceListRepository priceListRepository;
    private final PriceTierRepository priceTierRepository;

    @Override
    public PriceResultResponse resolve(UUID productId, UUID variantId, int quantity, SupportedCurrency currency) {
        return resolveInternal(productId, variantId, quantity, currency);
    }

    @Override
    public PriceResultResponse resolveForCheckout(UUID productId, UUID variantId, int quantity, SupportedCurrency currency) {
        // Checkout always bypasses cache and reads authoritative data.
        return resolveInternal(productId, variantId, quantity, currency);
    }

    // ── private ───────────────────────────────────────────────────────────────

    private PriceResultResponse resolveInternal(UUID productId, UUID variantId, int quantity, SupportedCurrency currency) {
        Long priceListId = defaultPriceListId();
        Instant now      = Instant.now();

        // Prefer variant-level price when variantId is supplied.
        Price price = null;
        if (variantId != null) {
            List<Price> variantPrices = priceRepository
                    .findEffectivePricesForVariant(priceListId, productId, variantId, currency, now);
            price = variantPrices.isEmpty() ? null : variantPrices.get(0);
        }

        if (price == null) {
            List<Price> productPrices = priceRepository
                    .findEffectivePricesForProduct(priceListId, productId, currency, now);
            if (productPrices.isEmpty()) {
                throw new PriceNotFoundException(productId);
            }
            price = productPrices.get(0);
        }

        // Apply tier pricing when tiers are defined and quantity > 1.
        BigDecimal effective = applyTier(price, quantity);

        BigDecimal base     = price.getAmount().setScale(2, RoundingMode.HALF_UP);
        effective           = effective.setScale(2, RoundingMode.HALF_UP);
        BigDecimal sale     = price.getSaleAmount() != null
                ? price.getSaleAmount().setScale(2, RoundingMode.HALF_UP) : null;

        BigDecimal discountAmount  = base.subtract(effective);
        BigDecimal discountPercent = base.compareTo(BigDecimal.ZERO) > 0
                ? discountAmount.divide(base, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        log.debug("Resolved price: product={} variant={} qty={} currency={} effective={}",
                productId, variantId, quantity, currency, effective);

        return PriceResultResponse.builder()
                .priceId(price.getPublicId())
                .productId(productId)
                .variantId(variantId)
                .basePrice(base)
                .salePrice(sale)
                .effectivePrice(effective)
                .discountAmount(discountAmount.max(BigDecimal.ZERO))
                .discountPercent(discountPercent.max(BigDecimal.ZERO))
                .currency(price.getCurrency().name())
                .validFrom(price.getValidFrom())
                .validUntil(price.getValidUntil())
                .build();
    }

    private BigDecimal applyTier(Price price, int quantity) {
        if (quantity <= 1) {
            return effectiveAmount(price);
        }
        List<PriceTier> tiers = priceTierRepository
                .findByPrice_IdAndIsActiveTrueOrderByMinQuantityAsc(price.getId());
        if (tiers.isEmpty()) {
            return effectiveAmount(price);
        }
        Optional<PriceTier> matchedTier = tiers.stream()
                .filter(t -> quantity >= t.getMinQuantity()
                          && (t.getMaxQuantity() == null || quantity <= t.getMaxQuantity()))
                .max(Comparator.comparingInt(PriceTier::getMinQuantity));
        return matchedTier.map(PriceTier::getUnitPrice).orElse(effectiveAmount(price));
    }

    private BigDecimal effectiveAmount(Price price) {
        return price.getSaleAmount() != null ? price.getSaleAmount() : price.getAmount();
    }

    private Long defaultPriceListId() {
        return priceListRepository.findByIsDefaultTrue()
                .orElseThrow(() -> new IllegalStateException("No default price list configured"))
                .getId();
    }
}

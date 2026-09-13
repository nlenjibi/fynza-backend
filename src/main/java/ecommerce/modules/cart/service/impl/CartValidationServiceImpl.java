package ecommerce.modules.cart.service.impl;

import ecommerce.modules.cart.dto.CartValidateRequest;
import ecommerce.modules.cart.dto.CartValidateResponse;
import ecommerce.modules.cart.dto.CartValidateResponse.CartItemValidationResult;
import ecommerce.modules.cart.service.CartValidationService;
import ecommerce.modules.pricing.dto.response.PriceResultResponse;
import ecommerce.modules.pricing.service.PriceResolverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartValidationServiceImpl implements CartValidationService {

    private final PriceResolverService priceResolverService;

    @Override
    public CartValidateResponse validate(CartValidateRequest request) {
        List<CartItemValidationResult> results = new ArrayList<>();
        boolean allValid = true;

        for (CartValidateRequest.CartValidateItem item : request.getItems()) {
            CartItemValidationResult result = validateItem(item);
            results.add(result);
            if (result.isPriceChanged()) {
                allValid = false;
            }
        }

        log.debug("Cart validation complete: valid={} items={}", allValid, results.size());

        return CartValidateResponse.builder()
                .valid(allValid)
                .items(results)
                .build();
    }

    // ── private ───────────────────────────────────────────────────────────────

    private CartItemValidationResult validateItem(CartValidateRequest.CartValidateItem item) {
        BigDecimal currentPrice;
        String message;
        boolean priceChanged;

        try {
            // Bypass cache — checkout must use authoritative price
            PriceResultResponse resolved = priceResolverService.resolveForCheckout(
                    item.getProductId(),
                    item.getVariantId(),
                    item.getQuantity(),
                    item.getCurrency());

            currentPrice = resolved.getEffectivePrice();
            BigDecimal expected = item.getExpectedPrice().setScale(2, RoundingMode.HALF_UP);
            BigDecimal current  = currentPrice.setScale(2, RoundingMode.HALF_UP);
            priceChanged = current.compareTo(expected) != 0;
            message = priceChanged
                    ? "Price changed from " + expected + " to " + current
                    : "Price confirmed";

        } catch (Exception e) {
            log.warn("Price resolution failed for product={} variant={}: {}",
                    item.getProductId(), item.getVariantId(), e.getMessage());
            currentPrice = null;
            priceChanged = true;
            message = "Price could not be resolved — item may no longer be available";
        }

        return CartItemValidationResult.builder()
                .productId(item.getProductId())
                .variantId(item.getVariantId())
                .quantity(item.getQuantity())
                .expectedPrice(item.getExpectedPrice())
                .currentPrice(currentPrice)
                .priceChanged(priceChanged)
                .currency(item.getCurrency().name())
                .message(message)
                .build();
    }
}

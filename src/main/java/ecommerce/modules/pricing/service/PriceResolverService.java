package ecommerce.modules.pricing.service;

import ecommerce.modules.pricing.dto.response.PriceResultResponse;
import ecommerce.modules.pricing.enums.SupportedCurrency;

import java.util.UUID;

public interface PriceResolverService {

    PriceResultResponse resolve(UUID productId, UUID variantId, int quantity, SupportedCurrency currency);

    PriceResultResponse resolveForCheckout(UUID productId, UUID variantId, int quantity, SupportedCurrency currency);
}

package ecommerce.modules.cart.service;

import ecommerce.modules.cart.dto.CartValidateRequest;
import ecommerce.modules.cart.dto.CartValidateRequest.CartValidateItem;
import ecommerce.modules.cart.dto.CartValidateResponse;
import ecommerce.modules.cart.dto.CartValidateResponse.CartItemValidationResult;
import ecommerce.modules.cart.service.impl.CartValidationServiceImpl;
import ecommerce.modules.pricing.dto.response.PriceResultResponse;
import ecommerce.modules.pricing.enums.SupportedCurrency;
import ecommerce.modules.pricing.exception.PriceNotFoundException;
import ecommerce.modules.pricing.service.PriceResolverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartValidationServiceImpl")
class CartValidationServiceImplTest {

    @Mock
    private PriceResolverService priceResolverService;

    @InjectMocks
    private CartValidationServiceImpl service;

    private UUID productIdA;
    private UUID productIdB;

    @BeforeEach
    void setUp() {
        productIdA = UUID.randomUUID();
        productIdB = UUID.randomUUID();
    }

    private CartValidateItem buildItem(UUID productId, UUID variantId,
                                       Integer quantity, SupportedCurrency currency,
                                       BigDecimal expectedPrice) {
        return CartValidateItem.builder()
                .productId(productId).variantId(variantId).quantity(quantity)
                .currency(currency).expectedPrice(expectedPrice).build();
    }

    private PriceResultResponse buildResolvedPrice(BigDecimal effectivePrice, SupportedCurrency currency) {
        return PriceResultResponse.builder()
                .priceId(UUID.randomUUID()).effectivePrice(effectivePrice)
                .basePrice(effectivePrice).discountAmount(BigDecimal.ZERO)
                .discountPercent(BigDecimal.ZERO).currency(currency.name()).build();
    }

    @Nested
    @DisplayName("all items match current prices")
    class AllItemsMatch {

        @Test
        @DisplayName("valid=true when single item price matches expected")
        void validate_singleItemPriceMatches_validTrue() {
            CartValidateItem item = buildItem(
                    productIdA, null, 2, SupportedCurrency.GHS, new BigDecimal("100.00"));

            when(priceResolverService.resolveForCheckout(
                    eq(productIdA), isNull(), eq(2), eq(SupportedCurrency.GHS)))
                    .thenReturn(buildResolvedPrice(new BigDecimal("100.00"), SupportedCurrency.GHS));

            CartValidateResponse response = service.validate(
                    CartValidateRequest.builder().items(List.of(item)).build());

            assertThat(response.isValid()).isTrue();
            assertThat(response.getItems()).hasSize(1);

            CartItemValidationResult result = response.getItems().get(0);
            assertThat(result.isPriceChanged()).isFalse();
            assertThat(result.getMessage()).isEqualTo("Price confirmed");
            assertThat(result.getCurrentPrice()).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("valid=true when all items match — one result per item")
        void validate_multipleItemsAllMatch_validTrue() {
            CartValidateItem itemA = buildItem(
                    productIdA, null, 1, SupportedCurrency.GHS, new BigDecimal("50.00"));
            CartValidateItem itemB = buildItem(
                    productIdB, null, 3, SupportedCurrency.GHS, new BigDecimal("25.00"));

            when(priceResolverService.resolveForCheckout(
                    eq(productIdA), isNull(), eq(1), eq(SupportedCurrency.GHS)))
                    .thenReturn(buildResolvedPrice(new BigDecimal("50.00"), SupportedCurrency.GHS));
            when(priceResolverService.resolveForCheckout(
                    eq(productIdB), isNull(), eq(3), eq(SupportedCurrency.GHS)))
                    .thenReturn(buildResolvedPrice(new BigDecimal("25.00"), SupportedCurrency.GHS));

            CartValidateResponse response = service.validate(
                    CartValidateRequest.builder().items(List.of(itemA, itemB)).build());

            assertThat(response.isValid()).isTrue();
            assertThat(response.getItems()).hasSize(2);
            assertThat(response.getItems()).allSatisfy(r -> assertThat(r.isPriceChanged()).isFalse());
        }

        @Test
        @DisplayName("price comparison is scale-insensitive: 100.0000 equals 100.00")
        void validate_differentScaleButEqualValue_priceNotChanged() {
            CartValidateItem item = buildItem(
                    productIdA, null, 1, SupportedCurrency.GHS, new BigDecimal("100.0000"));

            when(priceResolverService.resolveForCheckout(any(), any(), anyInt(), any()))
                    .thenReturn(buildResolvedPrice(new BigDecimal("100.00"), SupportedCurrency.GHS));

            CartValidateResponse response = service.validate(
                    CartValidateRequest.builder().items(List.of(item)).build());

            assertThat(response.isValid()).isTrue();
            assertThat(response.getItems().get(0).isPriceChanged()).isFalse();
        }
    }

    @Nested
    @DisplayName("price changed")
    class PriceChanged {

        @Test
        @DisplayName("valid=false and priceChanged=true when current price differs from expected")
        void validate_currentPriceDiffers_validFalseAndPriceChangedTrue() {
            CartValidateItem item = buildItem(
                    productIdA, null, 1, SupportedCurrency.GHS, new BigDecimal("100.00"));

            when(priceResolverService.resolveForCheckout(
                    eq(productIdA), isNull(), eq(1), eq(SupportedCurrency.GHS)))
                    .thenReturn(buildResolvedPrice(new BigDecimal("120.00"), SupportedCurrency.GHS));

            CartValidateResponse response = service.validate(
                    CartValidateRequest.builder().items(List.of(item)).build());

            assertThat(response.isValid()).isFalse();

            CartItemValidationResult result = response.getItems().get(0);
            assertThat(result.isPriceChanged()).isTrue();
            assertThat(result.getCurrentPrice()).isEqualByComparingTo("120.00");
            assertThat(result.getExpectedPrice()).isEqualByComparingTo("100.00");
            assertThat(result.getMessage()).contains("Price changed from");
            assertThat(result.getMessage()).contains("100.00");
            assertThat(result.getMessage()).contains("120.00");
        }

        @Test
        @DisplayName("valid=false even when only one of multiple items has a price change")
        void validate_oneOfTwoItemsPriceChanged_overallValidFalse() {
            CartValidateItem itemA = buildItem(
                    productIdA, null, 1, SupportedCurrency.GHS, new BigDecimal("50.00"));
            CartValidateItem itemB = buildItem(
                    productIdB, null, 2, SupportedCurrency.GHS, new BigDecimal("25.00"));

            when(priceResolverService.resolveForCheckout(
                    eq(productIdA), isNull(), eq(1), eq(SupportedCurrency.GHS)))
                    .thenReturn(buildResolvedPrice(new BigDecimal("50.00"), SupportedCurrency.GHS));
            when(priceResolverService.resolveForCheckout(
                    eq(productIdB), isNull(), eq(2), eq(SupportedCurrency.GHS)))
                    .thenReturn(buildResolvedPrice(new BigDecimal("20.00"), SupportedCurrency.GHS));

            CartValidateResponse response = service.validate(
                    CartValidateRequest.builder().items(List.of(itemA, itemB)).build());

            assertThat(response.isValid()).isFalse();

            CartItemValidationResult resultA = response.getItems().stream()
                    .filter(r -> r.getProductId().equals(productIdA)).findFirst().orElseThrow();
            CartItemValidationResult resultB = response.getItems().stream()
                    .filter(r -> r.getProductId().equals(productIdB)).findFirst().orElseThrow();

            assertThat(resultA.isPriceChanged()).isFalse();
            assertThat(resultB.isPriceChanged()).isTrue();
        }
    }

    @Nested
    @DisplayName("PriceNotFoundException during resolution")
    class PriceNotFoundDuringResolve {

        @Test
        @DisplayName("priceChanged=true and currentPrice=null when PriceNotFoundException thrown")
        void validate_priceNotFound_priceChangedTrueAndCurrentPriceNull() {
            CartValidateItem item = buildItem(
                    productIdA, null, 1, SupportedCurrency.GHS, new BigDecimal("50.00"));

            when(priceResolverService.resolveForCheckout(
                    eq(productIdA), isNull(), eq(1), eq(SupportedCurrency.GHS)))
                    .thenThrow(new PriceNotFoundException(productIdA));

            CartValidateResponse response = service.validate(
                    CartValidateRequest.builder().items(List.of(item)).build());

            assertThat(response.isValid()).isFalse();

            CartItemValidationResult result = response.getItems().get(0);
            assertThat(result.isPriceChanged()).isTrue();
            assertThat(result.getCurrentPrice()).isNull();
            assertThat(result.getMessage()).contains("Price could not be resolved");
        }

        @Test
        @DisplayName("any RuntimeException during resolve is treated as price unavailable")
        void validate_unexpectedExceptionDuringResolve_treatedAsPriceUnavailable() {
            CartValidateItem item = buildItem(
                    productIdA, null, 1, SupportedCurrency.GHS, new BigDecimal("50.00"));

            when(priceResolverService.resolveForCheckout(any(), any(), anyInt(), any()))
                    .thenThrow(new RuntimeException("DB timeout"));

            CartValidateResponse response = service.validate(
                    CartValidateRequest.builder().items(List.of(item)).build());

            assertThat(response.isValid()).isFalse();
            assertThat(response.getItems().get(0).isPriceChanged()).isTrue();
            assertThat(response.getItems().get(0).getCurrentPrice()).isNull();
        }
    }

    @Nested
    @DisplayName("mixed results across multiple items")
    class MixedResults {

        @Test
        @DisplayName("three items — one matched, one changed, one not-found — valid=false with three results")
        void validate_threeItemsMixedOutcomes_validFalseThreeResults() {
            UUID productIdC = UUID.randomUUID();

            CartValidateItem itemA = buildItem(
                    productIdA, null, 1, SupportedCurrency.GHS, new BigDecimal("100.00"));
            CartValidateItem itemB = buildItem(
                    productIdB, null, 2, SupportedCurrency.GHS, new BigDecimal("50.00"));
            CartValidateItem itemC = buildItem(
                    productIdC, null, 1, SupportedCurrency.GHS, new BigDecimal("30.00"));

            when(priceResolverService.resolveForCheckout(
                    eq(productIdA), isNull(), eq(1), eq(SupportedCurrency.GHS)))
                    .thenReturn(buildResolvedPrice(new BigDecimal("100.00"), SupportedCurrency.GHS));
            when(priceResolverService.resolveForCheckout(
                    eq(productIdB), isNull(), eq(2), eq(SupportedCurrency.GHS)))
                    .thenReturn(buildResolvedPrice(new BigDecimal("60.00"), SupportedCurrency.GHS));
            when(priceResolverService.resolveForCheckout(
                    eq(productIdC), isNull(), eq(1), eq(SupportedCurrency.GHS)))
                    .thenThrow(new PriceNotFoundException(productIdC));

            CartValidateResponse response = service.validate(
                    CartValidateRequest.builder().items(List.of(itemA, itemB, itemC)).build());

            assertThat(response.isValid()).isFalse();
            assertThat(response.getItems()).hasSize(3);

            long unchanged = response.getItems().stream().filter(r -> !r.isPriceChanged()).count();
            long changed   = response.getItems().stream().filter(CartItemValidationResult::isPriceChanged).count();

            assertThat(unchanged).isEqualTo(1);
            assertThat(changed).isEqualTo(2);
        }

        @Test
        @DisplayName("response items preserve productId, variantId, quantity, and currency from input")
        void validate_responseItemsPreserveInputIdentifiers() {
            UUID variantId = UUID.randomUUID();
            CartValidateItem item = buildItem(
                    productIdA, variantId, 5, SupportedCurrency.USD, new BigDecimal("45.00"));

            when(priceResolverService.resolveForCheckout(
                    eq(productIdA), eq(variantId), eq(5), eq(SupportedCurrency.USD)))
                    .thenReturn(buildResolvedPrice(new BigDecimal("45.00"), SupportedCurrency.USD));

            CartValidateResponse response = service.validate(
                    CartValidateRequest.builder().items(List.of(item)).build());

            CartItemValidationResult result = response.getItems().get(0);
            assertThat(result.getProductId()).isEqualTo(productIdA);
            assertThat(result.getVariantId()).isEqualTo(variantId);
            assertThat(result.getQuantity()).isEqualTo(5);
            assertThat(result.getCurrency()).isEqualTo("USD");
        }
    }
}

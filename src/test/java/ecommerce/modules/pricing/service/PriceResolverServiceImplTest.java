package ecommerce.modules.pricing.service;

import ecommerce.common.cache.RedisCacheService;
import ecommerce.modules.pricing.dto.response.PriceResultResponse;
import ecommerce.modules.pricing.entity.Price;
import ecommerce.modules.pricing.entity.PriceList;
import ecommerce.modules.pricing.entity.PriceTier;
import ecommerce.modules.pricing.enums.PriceStatus;
import ecommerce.modules.pricing.enums.SupportedCurrency;
import ecommerce.modules.pricing.exception.PriceNotFoundException;
import ecommerce.modules.pricing.repository.PriceListRepository;
import ecommerce.modules.pricing.repository.PriceRepository;
import ecommerce.modules.pricing.repository.PriceTierRepository;
import ecommerce.modules.pricing.service.impl.PriceResolverServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PriceResolverServiceImpl")
class PriceResolverServiceImplTest {

    @Mock private PriceRepository     priceRepository;
    @Mock private PriceListRepository priceListRepository;
    @Mock private PriceTierRepository priceTierRepository;
    @Mock private RedisCacheService   cacheService;

    @InjectMocks
    private PriceResolverServiceImpl service;

    private PriceList defaultPriceList;
    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        defaultPriceList = PriceList.builder()
                .id(1L).publicId(UUID.randomUUID()).name("Default")
                .isDefault(true).isActive(true).build();
    }

    private Price buildProductPrice(BigDecimal amount, BigDecimal saleAmount) {
        return Price.builder()
                .id(10L).publicId(UUID.randomUUID()).priceList(defaultPriceList)
                .productId(productId).amount(amount).saleAmount(saleAmount)
                .currency(SupportedCurrency.GHS).status(PriceStatus.ACTIVE).isActive(true).build();
    }

    private PriceTier buildTier(Price price, int minQty, Integer maxQty, BigDecimal unitPrice) {
        return PriceTier.builder()
                .id((long) minQty).publicId(UUID.randomUUID()).price(price)
                .minQuantity(minQty).maxQuantity(maxQty).unitPrice(unitPrice)
                .currency(SupportedCurrency.GHS).isActive(true).build();
    }

    @Nested
    @DisplayName("resolve — cache miss triggers DB lookup")
    class ResolveCacheMiss {

        @BeforeEach
        void stubCacheMiss() {
            when(cacheService.get(anyString(), eq(PriceResultResponse.class)))
                    .thenReturn(Optional.empty());
            when(priceListRepository.findByIsDefaultTrue()).thenReturn(Optional.of(defaultPriceList));
        }

        @Test
        @DisplayName("effectivePrice equals amount when no saleAmount and no tiers (quantity=1 skips tier lookup)")
        void resolve_noSaleNoTiers_effectiveEqualsAmount() {
            Price price = buildProductPrice(new BigDecimal("100.00"), null);
            when(priceRepository.findEffectivePricesForProduct(
                    eq(1L), eq(productId), eq(SupportedCurrency.GHS), any(Instant.class)))
                    .thenReturn(List.of(price));

            PriceResultResponse result = service.resolve(productId, null, 1, SupportedCurrency.GHS);

            assertThat(result.getEffectivePrice()).isEqualByComparingTo("100.00");
            assertThat(result.getBasePrice()).isEqualByComparingTo("100.00");
            assertThat(result.getSalePrice()).isNull();
            assertThat(result.getDiscountAmount()).isEqualByComparingTo("0.00");
            assertThat(result.getCurrency()).isEqualTo("GHS");
        }

        @Test
        @DisplayName("effectivePrice equals saleAmount when saleAmount is set and quantity is 1 (sale preferred via effectiveAmount)")
        void resolve_withSaleAmount_effectiveEqualsSaleAmount() {
            Price price = buildProductPrice(new BigDecimal("100.00"), new BigDecimal("75.00"));
            when(priceRepository.findEffectivePricesForProduct(
                    eq(1L), eq(productId), eq(SupportedCurrency.GHS), any(Instant.class)))
                    .thenReturn(List.of(price));

            PriceResultResponse result = service.resolve(productId, null, 1, SupportedCurrency.GHS);

            assertThat(result.getEffectivePrice()).isEqualByComparingTo("75.00");
            assertThat(result.getBasePrice()).isEqualByComparingTo("100.00");
            assertThat(result.getSalePrice()).isEqualByComparingTo("75.00");
            assertThat(result.getDiscountAmount()).isEqualByComparingTo("25.00");
        }

        @Test
        @DisplayName("throws PriceNotFoundException when no effective price exists for product")
        void resolve_noPriceForProduct_throwsPriceNotFoundException() {
            when(priceRepository.findEffectivePricesForProduct(
                    eq(1L), eq(productId), eq(SupportedCurrency.GHS), any(Instant.class)))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> service.resolve(productId, null, 1, SupportedCurrency.GHS))
                    .isInstanceOf(PriceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("resolve — cache hit returns cached result")
    class ResolveCacheHit {

        @Test
        @DisplayName("returns cached result without calling DB when cache hit")
        void resolve_cacheHit_returnsFromCacheWithoutDbCall() {
            PriceResultResponse cached = PriceResultResponse.builder()
                    .priceId(UUID.randomUUID()).productId(productId)
                    .effectivePrice(new BigDecimal("100.00")).basePrice(new BigDecimal("100.00"))
                    .discountAmount(BigDecimal.ZERO).discountPercent(BigDecimal.ZERO)
                    .currency("GHS").build();

            // resolveInternal is never called on cache hit — no DB calls needed
            when(cacheService.get(anyString(), eq(PriceResultResponse.class)))
                    .thenReturn(Optional.of(cached));

            PriceResultResponse result = service.resolve(productId, null, 1, SupportedCurrency.GHS);

            assertThat(result).isSameAs(cached);
            verifyNoInteractions(priceRepository);
            verify(cacheService, never()).put(anyString(), any(), any());
        }
    }

    @Nested
    @DisplayName("resolve — throws when no default price list")
    class NoDefaultPriceList {

        @Test
        @DisplayName("throws IllegalStateException when no default price list is configured")
        void resolve_noDefaultPriceList_throwsIllegalStateException() {
            when(cacheService.get(anyString(), eq(PriceResultResponse.class)))
                    .thenReturn(Optional.empty());
            when(priceListRepository.findByIsDefaultTrue()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resolve(productId, null, 1, SupportedCurrency.GHS))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No default price list");
        }
    }

    @Nested
    @DisplayName("resolve — variant preferred over product")
    class ResolveVariant {

        @BeforeEach
        void stubCacheMiss() {
            when(cacheService.get(anyString(), eq(PriceResultResponse.class)))
                    .thenReturn(Optional.empty());
            when(priceListRepository.findByIsDefaultTrue()).thenReturn(Optional.of(defaultPriceList));
        }

        @Test
        @DisplayName("returns variant-level price when variant prices are available")
        void resolve_variantPriceAvailable_returnsVariantPrice() {
            UUID variantId = UUID.randomUUID();
            Price variantPrice = Price.builder()
                    .id(20L).publicId(UUID.randomUUID()).priceList(defaultPriceList)
                    .productId(productId).variantId(variantId)
                    .amount(new BigDecimal("90.00")).currency(SupportedCurrency.GHS)
                    .status(PriceStatus.ACTIVE).isActive(true).build();

            when(priceRepository.findEffectivePricesForVariant(
                    eq(1L), eq(productId), eq(variantId), eq(SupportedCurrency.GHS), any(Instant.class)))
                    .thenReturn(List.of(variantPrice));
            // quantity=1 → priceTierRepository not called (applyTier short-circuits)

            PriceResultResponse result = service.resolve(productId, variantId, 1, SupportedCurrency.GHS);

            assertThat(result.getVariantId()).isEqualTo(variantId);
            assertThat(result.getEffectivePrice()).isEqualByComparingTo("90.00");
            verify(priceRepository, never())
                    .findEffectivePricesForProduct(any(), any(), any(), any());
        }

        @Test
        @DisplayName("falls back to product-level price when variant prices are empty")
        void resolve_noVariantPrice_fallsBackToProductPrice() {
            UUID variantId = UUID.randomUUID();
            Price productPrice = buildProductPrice(new BigDecimal("100.00"), null);

            when(priceRepository.findEffectivePricesForVariant(
                    eq(1L), eq(productId), eq(variantId), eq(SupportedCurrency.GHS), any(Instant.class)))
                    .thenReturn(List.of());
            when(priceRepository.findEffectivePricesForProduct(
                    eq(1L), eq(productId), eq(SupportedCurrency.GHS), any(Instant.class)))
                    .thenReturn(List.of(productPrice));
            // quantity=1 → priceTierRepository not called

            PriceResultResponse result = service.resolve(productId, variantId, 1, SupportedCurrency.GHS);

            assertThat(result.getEffectivePrice()).isEqualByComparingTo("100.00");
        }
    }

    @Nested
    @DisplayName("tier pricing")
    class TierPricing {

        @BeforeEach
        void stubCacheMiss() {
            when(cacheService.get(anyString(), eq(PriceResultResponse.class)))
                    .thenReturn(Optional.empty());
            when(priceListRepository.findByIsDefaultTrue()).thenReturn(Optional.of(defaultPriceList));
        }

        @Test
        @DisplayName("selects tier whose quantity range covers the requested quantity")
        void resolve_quantityMatchesTier_tierPriceApplied() {
            Price price = buildProductPrice(new BigDecimal("100.00"), null);
            PriceTier tier = buildTier(price, 10, 49, new BigDecimal("88.00"));

            when(priceRepository.findEffectivePricesForProduct(
                    eq(1L), eq(productId), eq(SupportedCurrency.GHS), any(Instant.class)))
                    .thenReturn(List.of(price));
            when(priceTierRepository.findByPrice_IdAndIsActiveTrueOrderByMinQuantityAsc(price.getId()))
                    .thenReturn(List.of(tier));

            PriceResultResponse result = service.resolve(productId, null, 20, SupportedCurrency.GHS);

            assertThat(result.getEffectivePrice()).isEqualByComparingTo("88.00");
            assertThat(result.getDiscountAmount()).isEqualByComparingTo("12.00");
        }

        @Test
        @DisplayName("selects highest minQuantity tier when multiple tiers match the quantity")
        void resolve_multipleMatchingTiers_highestMinQuantityWins() {
            Price price = buildProductPrice(new BigDecimal("100.00"), null);
            PriceTier tierA = buildTier(price, 10, 99, new BigDecimal("90.00"));
            PriceTier tierB = buildTier(price, 50, 99, new BigDecimal("80.00"));

            when(priceRepository.findEffectivePricesForProduct(
                    eq(1L), eq(productId), eq(SupportedCurrency.GHS), any(Instant.class)))
                    .thenReturn(List.of(price));
            when(priceTierRepository.findByPrice_IdAndIsActiveTrueOrderByMinQuantityAsc(price.getId()))
                    .thenReturn(List.of(tierA, tierB));

            PriceResultResponse result = service.resolve(productId, null, 60, SupportedCurrency.GHS);

            assertThat(result.getEffectivePrice()).isEqualByComparingTo("80.00");
        }

        @Test
        @DisplayName("falls back to base price when quantity exceeds all tier maxQuantity bounds")
        void resolve_quantityBeyondAllTiers_fallsBackToBasePrice() {
            Price price = buildProductPrice(new BigDecimal("100.00"), null);
            PriceTier tier = buildTier(price, 10, 20, new BigDecimal("88.00"));

            when(priceRepository.findEffectivePricesForProduct(
                    eq(1L), eq(productId), eq(SupportedCurrency.GHS), any(Instant.class)))
                    .thenReturn(List.of(price));
            when(priceTierRepository.findByPrice_IdAndIsActiveTrueOrderByMinQuantityAsc(price.getId()))
                    .thenReturn(List.of(tier));

            PriceResultResponse result = service.resolve(productId, null, 100, SupportedCurrency.GHS);

            assertThat(result.getEffectivePrice()).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("tier with null maxQuantity matches any quantity at or above minQuantity")
        void resolve_openEndedTier_matchesAnyQuantityAboveMin() {
            Price price = buildProductPrice(new BigDecimal("100.00"), null);
            PriceTier openTier = buildTier(price, 5, null, new BigDecimal("85.00"));

            when(priceRepository.findEffectivePricesForProduct(
                    eq(1L), eq(productId), eq(SupportedCurrency.GHS), any(Instant.class)))
                    .thenReturn(List.of(price));
            when(priceTierRepository.findByPrice_IdAndIsActiveTrueOrderByMinQuantityAsc(price.getId()))
                    .thenReturn(List.of(openTier));

            PriceResultResponse result = service.resolve(productId, null, 500, SupportedCurrency.GHS);

            assertThat(result.getEffectivePrice()).isEqualByComparingTo("85.00");
        }

        @Test
        @DisplayName("tiers are not consulted when quantity is 1 — applyTier short-circuits")
        void resolve_quantityOne_tiersNotConsulted() {
            Price price = buildProductPrice(new BigDecimal("100.00"), null);
            when(priceRepository.findEffectivePricesForProduct(
                    eq(1L), eq(productId), eq(SupportedCurrency.GHS), any(Instant.class)))
                    .thenReturn(List.of(price));

            PriceResultResponse result = service.resolve(productId, null, 1, SupportedCurrency.GHS);

            assertThat(result.getEffectivePrice()).isEqualByComparingTo("100.00");
            verify(priceTierRepository, never())
                    .findByPrice_IdAndIsActiveTrueOrderByMinQuantityAsc(anyLong());
        }
    }

    @Nested
    @DisplayName("resolveForCheckout — always reads DB")
    class ResolveForCheckout {

        @Test
        @DisplayName("resolveForCheckout reads DB without touching cache")
        void resolveForCheckout_alwaysReadFromDb() {
            Price price = buildProductPrice(new BigDecimal("200.00"), null);
            when(priceListRepository.findByIsDefaultTrue()).thenReturn(Optional.of(defaultPriceList));
            when(priceRepository.findEffectivePricesForProduct(
                    eq(1L), eq(productId), eq(SupportedCurrency.GHS), any(Instant.class)))
                    .thenReturn(List.of(price));
            // quantity=1 → priceTierRepository not called

            PriceResultResponse result = service.resolveForCheckout(productId, null, 1, SupportedCurrency.GHS);

            assertThat(result.getEffectivePrice()).isEqualByComparingTo("200.00");
            verifyNoInteractions(cacheService);
        }

        @Test
        @DisplayName("resolveForCheckout throws PriceNotFoundException when no price exists")
        void resolveForCheckout_noPrice_throwsPriceNotFoundException() {
            when(priceListRepository.findByIsDefaultTrue()).thenReturn(Optional.of(defaultPriceList));
            when(priceRepository.findEffectivePricesForProduct(
                    eq(1L), eq(productId), eq(SupportedCurrency.GHS), any(Instant.class)))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> service.resolveForCheckout(productId, null, 1, SupportedCurrency.GHS))
                    .isInstanceOf(PriceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("currency filtering")
    class CurrencyFiltering {

        @BeforeEach
        void stubCacheMiss() {
            when(cacheService.get(anyString(), eq(PriceResultResponse.class)))
                    .thenReturn(Optional.empty());
            when(priceListRepository.findByIsDefaultTrue()).thenReturn(Optional.of(defaultPriceList));
        }

        @Test
        @DisplayName("throws PriceNotFoundException when no price exists for the requested currency")
        void resolve_noPriceForRequestedCurrency_throwsPriceNotFoundException() {
            when(priceRepository.findEffectivePricesForProduct(
                    eq(1L), eq(productId), eq(SupportedCurrency.USD), any(Instant.class)))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> service.resolve(productId, null, 1, SupportedCurrency.USD))
                    .isInstanceOf(PriceNotFoundException.class);
        }

        @Test
        @DisplayName("returns USD price when USD is requested and configured")
        void resolve_usdPriceConfigured_returnsUsdPrice() {
            Price usdPrice = Price.builder()
                    .id(30L).publicId(UUID.randomUUID()).priceList(defaultPriceList)
                    .productId(productId).amount(new BigDecimal("25.00"))
                    .currency(SupportedCurrency.USD).status(PriceStatus.ACTIVE).isActive(true).build();

            when(priceRepository.findEffectivePricesForProduct(
                    eq(1L), eq(productId), eq(SupportedCurrency.USD), any(Instant.class)))
                    .thenReturn(List.of(usdPrice));
            // quantity=1 → priceTierRepository not called

            PriceResultResponse result = service.resolve(productId, null, 1, SupportedCurrency.USD);

            assertThat(result.getCurrency()).isEqualTo("USD");
            assertThat(result.getEffectivePrice()).isEqualByComparingTo("25.00");
        }
    }
}

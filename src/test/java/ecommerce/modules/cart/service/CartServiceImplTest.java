package ecommerce.modules.cart.service;

import ecommerce.common.enums.CouponStatus;
import ecommerce.common.enums.DiscountType;
import ecommerce.common.enums.ProductStatus;
import ecommerce.modules.cart.dto.*;
import ecommerce.modules.cart.entity.Cart;
import ecommerce.modules.cart.entity.CartItem;
import ecommerce.modules.cart.entity.CartStatus;
import ecommerce.modules.cart.event.*;
import ecommerce.modules.cart.exception.CartErrorCode;
import ecommerce.modules.cart.exception.CartException;
import ecommerce.modules.cart.repository.CartItemRepository;
import ecommerce.modules.cart.repository.CartRepository;
import ecommerce.modules.cart.repository.StockReservationRepository;
import ecommerce.modules.cart.service.impl.CartServiceImpl;
import ecommerce.modules.coupon.entity.Coupon;
import ecommerce.modules.coupon.repository.CouponRepository;
import ecommerce.modules.inventory.dto.response.AvailabilityResponse;
import ecommerce.modules.inventory.service.InventoryService;
import ecommerce.modules.pricing.dto.response.PriceResultResponse;
import ecommerce.modules.pricing.enums.SupportedCurrency;
import ecommerce.modules.pricing.service.PriceResolverService;
import ecommerce.modules.product.entity.Product;
import ecommerce.modules.product.entity.ProductVariant;
import ecommerce.modules.product.repository.ProductRepository;
import ecommerce.modules.product.repository.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartServiceImpl")
class CartServiceImplTest {

    @Mock private CartRepository             cartRepository;
    @Mock private CartItemRepository         cartItemRepository;
    @Mock private StockReservationRepository reservationRepository;
    @Mock private CouponRepository           couponRepository;
    @Mock private PriceResolverService       priceResolverService;
    @Mock private ProductRepository          productRepository;
    @Mock private ProductVariantRepository   variantRepository;
    @Mock private InventoryService           inventoryService;
    @Mock private CartEventPublisher         eventPublisher;

    @InjectMocks
    private CartServiceImpl service;

    private UUID userId;
    private UUID productId;
    private UUID variantId;

    @BeforeEach
    void setUp() {
        userId    = UUID.randomUUID();
        productId = UUID.randomUUID();
        variantId = UUID.randomUUID();
    }

    // ── Builders ────────────────────────────────────────────────────────────────

    private Cart buildCart(UUID ownerId) {
        return Cart.builder()
                .id(1L)
                .publicId(UUID.randomUUID())
                .userId(ownerId)
                .isGuest(false)
                .status(CartStatus.ACTIVE)
                .subtotal(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .shippingTotal(BigDecimal.ZERO)
                .taxTotal(BigDecimal.ZERO)
                .grandTotal(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();
    }

    private CartItem buildCartItem(Cart cart, UUID pid, int qty, BigDecimal price) {
        return CartItem.builder()
                .id(1L)
                .publicId(UUID.randomUUID())
                .cart(cart)
                .productId(pid)
                .quantity(qty)
                .unitPrice(price)
                .lineTotal(price.multiply(BigDecimal.valueOf(qty)))
                .priceChanged(false)
                .build();
    }

    private Product activeProduct() {
        return Product.builder()
                .id(productId)
                .isActive(true)
                .status(ProductStatus.ACTIVE)
                .storeId(10L)
                .name("Widget")
                .slug("widget")
                .build();
    }

    private ProductVariant activeVariant() {
        return ProductVariant.builder()
                .id(variantId)
                .productId(productId)
                .isActive(true)
                .variantStatus("ACTIVE")
                .sku("SKU-001")
                .build();
    }

    private AvailabilityResponse sufficientStock(int qty) {
        return AvailabilityResponse.builder()
                .productId(productId)
                .availableQuantity(qty)
                .allowBackorder(false)
                .build();
    }

    private PriceResultResponse priceResult(BigDecimal price) {
        return PriceResultResponse.builder()
                .effectivePrice(price)
                .build();
    }

    private void stubCartSave() {
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));
    }

    private void stubItemSave() {
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(i -> i.getArgument(0));
    }

    private void stubFindByCartId(Long cartId, List<CartItem> items) {
        when(cartItemRepository.findByCartId(cartId)).thenReturn(items);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // createGuestCart
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createGuestCart")
    class CreateGuestCart {

        @Test
        @DisplayName("createGuestCart_success")
        void createGuestCart_success() {
            stubCartSave();

            GuestCartResponse response = service.createGuestCart();

            ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
            verify(cartRepository).save(captor.capture());
            Cart saved = captor.getValue();

            assertThat(saved.getIsGuest()).isTrue();
            assertThat(saved.getStatus()).isEqualTo(CartStatus.ACTIVE);
            assertThat(saved.getCartToken()).hasSize(64);
            assertThat(saved.getExpiresAt()).isAfter(Instant.now());
            assertThat(response.getCartToken()).hasSize(64);
            verify(eventPublisher).publish(any(GuestCartCreatedEvent.class));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // getCart
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getCart")
    class GetCart {

        @Test
        @DisplayName("getCart_existingCart_returnsResponse")
        void getCart_existingCart_returnsResponse() {
            Cart cart = buildCart(userId);
            when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(cart));
            stubFindByCartId(cart.getId(), List.of());

            CartResponse response = service.getCart(userId);

            assertThat(response.getUserId()).isEqualTo(userId);
            assertThat(response.getStatus()).isEqualTo(CartStatus.ACTIVE);
            verify(cartRepository, never()).save(any());
        }

        @Test
        @DisplayName("getCart_noCart_createsNewCart")
        void getCart_noCart_createsNewCart() {
            when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.empty());
            stubCartSave();
            when(cartItemRepository.findByCartId(any())).thenReturn(List.of());

            service.getCart(userId);

            ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
            verify(cartRepository).save(captor.capture());
            Cart created = captor.getValue();
            assertThat(created.getUserId()).isEqualTo(userId);
            assertThat(created.getIsGuest()).isFalse();
            assertThat(created.getStatus()).isEqualTo(CartStatus.ACTIVE);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // getGuestCart
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getGuestCart")
    class GetGuestCart {

        @Test
        @DisplayName("getGuestCart_success")
        void getGuestCart_success() {
            Cart cart = Cart.builder()
                    .id(2L)
                    .publicId(UUID.randomUUID())
                    .isGuest(true)
                    .cartToken("abc-token")
                    .status(CartStatus.ACTIVE)
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .subtotal(BigDecimal.ZERO)
                    .discountAmount(BigDecimal.ZERO)
                    .shippingTotal(BigDecimal.ZERO)
                    .taxTotal(BigDecimal.ZERO)
                    .grandTotal(BigDecimal.ZERO)
                    .items(new ArrayList<>())
                    .build();

            when(cartRepository.findByCartToken("abc-token")).thenReturn(Optional.of(cart));
            stubFindByCartId(cart.getId(), List.of());

            CartResponse response = service.getGuestCart("abc-token");

            assertThat(response.getCartToken()).isEqualTo("abc-token");
            assertThat(response.getIsGuest()).isTrue();
        }

        @Test
        @DisplayName("getGuestCart_tokenNotFound_throwsCartException")
        void getGuestCart_tokenNotFound_throwsCartException() {
            when(cartRepository.findByCartToken("bad-token")).thenReturn(Optional.empty());

            CartException ex = assertThrows(CartException.class,
                    () -> service.getGuestCart("bad-token"));

            assertThat(ex.getErrorCode()).isEqualTo(CartErrorCode.GUEST_CART_NOT_FOUND);
        }

        @Test
        @DisplayName("getGuestCart_expiredCart_throwsCartExpired")
        void getGuestCart_expiredCart_throwsCartExpired() {
            Cart expired = Cart.builder()
                    .id(3L)
                    .publicId(UUID.randomUUID())
                    .isGuest(true)
                    .cartToken("expired-token")
                    .status(CartStatus.ACTIVE)
                    .expiresAt(Instant.now().minusSeconds(1))
                    .items(new ArrayList<>())
                    .build();

            when(cartRepository.findByCartToken("expired-token")).thenReturn(Optional.of(expired));
            when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));

            CartException ex = assertThrows(CartException.class,
                    () -> service.getGuestCart("expired-token"));

            assertThat(ex.getErrorCode()).isEqualTo(CartErrorCode.CART_EXPIRED);
            assertThat(expired.getStatus()).isEqualTo(CartStatus.EXPIRED);
            verify(cartRepository).save(expired);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // addItem
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("addItem")
    class AddItem {

        private AddToCartRequest request(UUID pid, UUID vid, int qty) {
            return AddToCartRequest.builder()
                    .productId(pid)
                    .variantId(vid)
                    .quantity(qty)
                    .build();
        }

        @Test
        @DisplayName("addItem_productNotFound_throwsProductNotFound")
        void addItem_productNotFound_throwsProductNotFound() {
            Cart cart = buildCart(userId);
            when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(cart));
            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            CartException ex = assertThrows(CartException.class,
                    () -> service.addItem(userId, request(productId, null, 1)));

            assertThat(ex.getErrorCode()).isEqualTo(CartErrorCode.PRODUCT_NOT_FOUND);
        }

        @Test
        @DisplayName("addItem_productInactive_throwsItemUnavailable")
        void addItem_productInactive_throwsItemUnavailable() {
            Cart cart = buildCart(userId);
            when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(cart));

            Product inactive = Product.builder()
                    .id(productId)
                    .isActive(false)
                    .status(ProductStatus.INACTIVE)
                    .storeId(10L)
                    .name("Widget")
                    .slug("widget")
                    .build();
            when(productRepository.findById(productId)).thenReturn(Optional.of(inactive));

            CartException ex = assertThrows(CartException.class,
                    () -> service.addItem(userId, request(productId, null, 1)));

            assertThat(ex.getErrorCode()).isEqualTo(CartErrorCode.ITEM_UNAVAILABLE);
        }

        @Test
        @DisplayName("addItem_variantNotFound_throwsVariantNotFound")
        void addItem_variantNotFound_throwsVariantNotFound() {
            Cart cart = buildCart(userId);
            when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(cart));
            when(productRepository.findById(productId)).thenReturn(Optional.of(activeProduct()));
            when(variantRepository.findByIdAndProductId(variantId, productId))
                    .thenReturn(Optional.empty());

            CartException ex = assertThrows(CartException.class,
                    () -> service.addItem(userId, request(productId, variantId, 1)));

            assertThat(ex.getErrorCode()).isEqualTo(CartErrorCode.VARIANT_NOT_FOUND);
        }

        @Test
        @DisplayName("addItem_insufficientStock_throwsItemUnavailable")
        void addItem_insufficientStock_throwsItemUnavailable() {
            Cart cart = buildCart(userId);
            when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(cart));
            when(productRepository.findById(productId)).thenReturn(Optional.of(activeProduct()));
            when(variantRepository.findByIdAndProductId(variantId, productId))
                    .thenReturn(Optional.of(activeVariant()));

            AvailabilityResponse lowStock = AvailabilityResponse.builder()
                    .productId(productId)
                    .availableQuantity(2)
                    .allowBackorder(false)
                    .build();
            when(inventoryService.getAvailability(productId, variantId)).thenReturn(lowStock);

            CartException ex = assertThrows(CartException.class,
                    () -> service.addItem(userId, request(productId, variantId, 10)));

            assertThat(ex.getErrorCode()).isEqualTo(CartErrorCode.ITEM_UNAVAILABLE);
        }

        @Test
        @DisplayName("addItem_newItem_success")
        void addItem_newItem_success() {
            Cart cart = buildCart(userId);
            when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(cart));
            when(productRepository.findById(productId)).thenReturn(Optional.of(activeProduct()));
            when(inventoryService.getAvailability(productId, null)).thenReturn(sufficientStock(50));
            when(priceResolverService.resolve(eq(productId), isNull(), eq(2), eq(SupportedCurrency.GHS)))
                    .thenReturn(priceResult(new BigDecimal("25.00")));
            when(cartItemRepository.findByCartIdAndProductAndVariant(cart.getId(), productId, null))
                    .thenReturn(Optional.empty());
            stubItemSave();
            stubCartSave();
            stubFindByCartId(cart.getId(), List.of());

            CartItemResponse response = service.addItem(userId, request(productId, null, 2));

            verify(cartItemRepository).save(any(CartItem.class));
            verify(cartRepository).save(cart);
            verify(eventPublisher).publish(any(CartItemAddedEvent.class));
            assertThat(response.getProductId()).isEqualTo(productId);
            assertThat(response.getQuantity()).isEqualTo(2);
            assertThat(response.getUnitPrice()).isEqualByComparingTo("25.00");
        }

        @Test
        @DisplayName("addItem_existingItem_mergesQuantity")
        void addItem_existingItem_mergesQuantity() {
            Cart cart = buildCart(userId);
            CartItem existing = buildCartItem(cart, productId, 3, new BigDecimal("25.00"));

            when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(cart));
            when(productRepository.findById(productId)).thenReturn(Optional.of(activeProduct()));
            when(inventoryService.getAvailability(productId, null))
                    .thenReturn(sufficientStock(100));
            when(priceResolverService.resolve(eq(productId), isNull(), eq(2), eq(SupportedCurrency.GHS)))
                    .thenReturn(priceResult(new BigDecimal("25.00")));
            when(cartItemRepository.findByCartIdAndProductAndVariant(cart.getId(), productId, null))
                    .thenReturn(Optional.of(existing));
            stubItemSave();
            stubCartSave();
            stubFindByCartId(cart.getId(), List.of(existing));

            service.addItem(userId, request(productId, null, 2));

            assertThat(existing.getQuantity()).isEqualTo(5);
            verify(cartItemRepository).save(existing);
            verify(eventPublisher).publish(any(CartItemAddedEvent.class));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // updateItemQuantity
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateItemQuantity")
    class UpdateItemQuantity {

        @Test
        @DisplayName("updateItemQuantity_success")
        void updateItemQuantity_success() {
            Cart cart = buildCart(userId);
            UUID itemPublicId = UUID.randomUUID();
            CartItem item = buildCartItem(cart, productId, 2, new BigDecimal("20.00"));
            item.setPublicId(itemPublicId);

            when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findByCartIdAndPublicId(cart.getId(), itemPublicId))
                    .thenReturn(Optional.of(item));
            stubItemSave();
            stubCartSave();
            stubFindByCartId(cart.getId(), List.of(item));

            CartItemResponse response = service.updateItemQuantity(userId, itemPublicId, 7);

            assertThat(item.getQuantity()).isEqualTo(7);
            assertThat(response.getQuantity()).isEqualTo(7);
            verify(cartItemRepository).save(item);
            verify(cartRepository).save(cart);
            verify(eventPublisher).publish(any(CartItemUpdatedEvent.class));
        }

        @Test
        @DisplayName("updateItemQuantity_itemNotFound_throwsCartException")
        void updateItemQuantity_itemNotFound_throwsCartException() {
            Cart cart = buildCart(userId);
            UUID itemPublicId = UUID.randomUUID();

            when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findByCartIdAndPublicId(cart.getId(), itemPublicId))
                    .thenReturn(Optional.empty());

            CartException ex = assertThrows(CartException.class,
                    () -> service.updateItemQuantity(userId, itemPublicId, 5));

            assertThat(ex.getErrorCode()).isEqualTo(CartErrorCode.ITEM_NOT_FOUND);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // removeItem
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("removeItem")
    class RemoveItem {

        @Test
        @DisplayName("removeItem_success")
        void removeItem_success() {
            Cart cart = buildCart(userId);
            UUID itemPublicId = UUID.randomUUID();
            CartItem item = buildCartItem(cart, productId, 1, new BigDecimal("10.00"));
            item.setPublicId(itemPublicId);

            when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findByCartIdAndPublicId(cart.getId(), itemPublicId))
                    .thenReturn(Optional.of(item));
            when(reservationRepository.findByCartItemId(item.getId())).thenReturn(Optional.empty());
            stubCartSave();
            stubFindByCartId(cart.getId(), List.of());

            service.removeItem(userId, itemPublicId);

            verify(cartItemRepository).delete(item);
            verify(cartRepository).save(cart);
            verify(eventPublisher).publish(any(CartItemRemovedEvent.class));
        }

        @Test
        @DisplayName("removeItem_itemNotFound_throwsCartException")
        void removeItem_itemNotFound_throwsCartException() {
            Cart cart = buildCart(userId);
            UUID itemPublicId = UUID.randomUUID();

            when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findByCartIdAndPublicId(cart.getId(), itemPublicId))
                    .thenReturn(Optional.empty());

            CartException ex = assertThrows(CartException.class,
                    () -> service.removeItem(userId, itemPublicId));

            assertThat(ex.getErrorCode()).isEqualTo(CartErrorCode.ITEM_NOT_FOUND);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // applyCoupon
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("applyCoupon")
    class ApplyCoupon {

        private Coupon buildActiveCoupon(String code) {
            return Coupon.builder()
                    .id(1L)
                    .publicId(UUID.randomUUID())
                    .code(code)
                    .status(CouponStatus.ACTIVE)
                    .discountType(DiscountType.PERCENTAGE)
                    .discountValue(new BigDecimal("10"))
                    .validFrom(LocalDateTime.now().minusDays(1))
                    .validUntil(LocalDateTime.now().plusDays(30))
                    .maxUses(100)
                    .usageCount(0)
                    .build();
        }

        @Test
        @DisplayName("applyCoupon_couponNotFound_throwsCartException")
        void applyCoupon_couponNotFound_throwsCartException() {
            Cart cart = buildCart(userId);
            when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(cart));
            when(couponRepository.findByCode("NOPE")).thenReturn(Optional.empty());

            CartException ex = assertThrows(CartException.class,
                    () -> service.applyCoupon(userId, "NOPE"));

            assertThat(ex.getErrorCode()).isEqualTo(CartErrorCode.COUPON_NOT_FOUND);
        }

        @Test
        @DisplayName("applyCoupon_inactiveCoupon_throwsCartException")
        void applyCoupon_inactiveCoupon_throwsCartException() {
            Cart cart = buildCart(userId);
            Coupon inactive = buildActiveCoupon("OFF10");
            inactive.setStatus(CouponStatus.INACTIVE);

            when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(cart));
            when(couponRepository.findByCode("OFF10")).thenReturn(Optional.of(inactive));

            CartException ex = assertThrows(CartException.class,
                    () -> service.applyCoupon(userId, "OFF10"));

            assertThat(ex.getErrorCode()).isEqualTo(CartErrorCode.COUPON_INACTIVE);
        }

        @Test
        @DisplayName("applyCoupon_expiredCoupon_throwsCartException")
        void applyCoupon_expiredCoupon_throwsCartException() {
            Cart cart = buildCart(userId);
            Coupon expired = buildActiveCoupon("EXP10");
            expired.setValidFrom(LocalDateTime.now().minusDays(10));
            expired.setValidUntil(LocalDateTime.now().minusDays(1));

            when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(cart));
            when(couponRepository.findByCode("EXP10")).thenReturn(Optional.of(expired));

            CartException ex = assertThrows(CartException.class,
                    () -> service.applyCoupon(userId, "EXP10"));

            assertThat(ex.getErrorCode()).isEqualTo(CartErrorCode.COUPON_EXPIRED);
        }

        @Test
        @DisplayName("applyCoupon_usageLimitReached_throwsCartException")
        void applyCoupon_usageLimitReached_throwsCartException() {
            Cart cart = buildCart(userId);
            Coupon maxed = buildActiveCoupon("MAX10");
            maxed.setMaxUses(50);
            maxed.setUsageCount(50);

            when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(cart));
            when(couponRepository.findByCode("MAX10")).thenReturn(Optional.of(maxed));

            CartException ex = assertThrows(CartException.class,
                    () -> service.applyCoupon(userId, "MAX10"));

            assertThat(ex.getErrorCode()).isEqualTo(CartErrorCode.COUPON_USAGE_LIMIT_REACHED);
        }

        @Test
        @DisplayName("applyCoupon_success_percentageDiscount")
        void applyCoupon_success_percentageDiscount() {
            Cart cart = buildCart(userId);
            cart.setSubtotal(new BigDecimal("100.00"));
            Coupon coupon = buildActiveCoupon("SAVE10");

            when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));
            when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(cart));
            stubCartSave();
            stubFindByCartId(cart.getId(), List.of());

            service.applyCoupon(userId, "SAVE10");

            assertThat(cart.getCouponCode()).isEqualTo("SAVE10");
            verify(cartRepository).save(cart);
            verify(eventPublisher).publish(any(CouponAppliedEvent.class));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // removeCoupon
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("removeCoupon")
    class RemoveCoupon {

        @Test
        @DisplayName("removeCoupon_withExistingCoupon_clearsCoupon")
        void removeCoupon_withExistingCoupon_clearsCoupon() {
            Cart cart = buildCart(userId);
            cart.setCouponCode("SAVE10");
            cart.setDiscountAmount(new BigDecimal("10.00"));

            when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(cart));
            stubCartSave();
            stubFindByCartId(cart.getId(), List.of());

            service.removeCoupon(userId);

            assertThat(cart.getCouponCode()).isNull();
            assertThat(cart.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            verify(cartRepository).save(cart);
            verify(eventPublisher).publish(any(CouponRemovedEvent.class));
        }

        @Test
        @DisplayName("removeCoupon_withNoCoupon_doesNotPublishEvent")
        void removeCoupon_withNoCoupon_doesNotPublishEvent() {
            Cart cart = buildCart(userId);

            when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(cart));
            stubCartSave();
            stubFindByCartId(cart.getId(), List.of());

            service.removeCoupon(userId);

            verify(eventPublisher, never()).publish(any(CouponRemovedEvent.class));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // clearCart
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("clearCart")
    class ClearCart {

        @Test
        @DisplayName("clearCart_success")
        void clearCart_success() {
            Cart cart = buildCart(userId);
            CartItem item = buildCartItem(cart, productId, 2, new BigDecimal("15.00"));
            cart.getItems().add(item);
            cart.setCouponCode("CODE");

            when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(cart));
            stubCartSave();
            stubFindByCartId(cart.getId(), List.of());

            service.clearCart(userId);

            verify(reservationRepository).deleteByCartId(cart.getId());
            verify(cartItemRepository).deleteByCartId(cart.getId());
            assertThat(cart.getCouponCode()).isNull();
            verify(cartRepository).save(cart);
            verify(eventPublisher).publish(any(CartClearedEvent.class));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // mergeCart
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("mergeCart")
    class MergeCart {

        @Test
        @DisplayName("mergeCart_success")
        void mergeCart_success() {
            Cart userCart = buildCart(userId);
            Cart guestCart = Cart.builder()
                    .id(2L)
                    .publicId(UUID.randomUUID())
                    .isGuest(true)
                    .cartToken("guest-token")
                    .status(CartStatus.ACTIVE)
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .subtotal(BigDecimal.ZERO)
                    .discountAmount(BigDecimal.ZERO)
                    .shippingTotal(BigDecimal.ZERO)
                    .taxTotal(BigDecimal.ZERO)
                    .grandTotal(BigDecimal.ZERO)
                    .items(new ArrayList<>())
                    .build();

            CartItem guestItem = buildCartItem(guestCart, productId, 3, new BigDecimal("10.00"));

            when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(userCart));
            when(cartRepository.findByCartToken("guest-token")).thenReturn(Optional.of(guestCart));
            when(cartItemRepository.findByCartId(guestCart.getId())).thenReturn(List.of(guestItem));
            when(productRepository.findById(productId)).thenReturn(Optional.of(activeProduct()));
            when(inventoryService.getAvailability(productId, null)).thenReturn(sufficientStock(100));
            when(priceResolverService.resolve(eq(productId), isNull(), eq(3), eq(SupportedCurrency.GHS)))
                    .thenReturn(priceResult(new BigDecimal("10.00")));
            when(cartItemRepository.findByCartIdAndProductAndVariant(userCart.getId(), productId, null))
                    .thenReturn(Optional.empty());
            stubItemSave();
            stubCartSave();
            when(cartItemRepository.findByCartId(userCart.getId())).thenReturn(List.of());

            service.mergeCart(userId, "guest-token");

            assertThat(guestCart.getStatus()).isEqualTo(CartStatus.MERGED);
            assertThat(guestCart.getMergedIntoCartId()).isEqualTo(userCart.getId());
            verify(cartRepository, atLeastOnce()).save(guestCart);
            verify(cartRepository, atLeastOnce()).save(userCart);
            verify(eventPublisher).publish(any(CartMergedEvent.class));
        }

        @Test
        @DisplayName("mergeCart_guestTokenNotFound_throwsCartException")
        void mergeCart_guestTokenNotFound_throwsCartException() {
            Cart userCart = buildCart(userId);
            when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(userCart));
            when(cartRepository.findByCartToken("invalid")).thenReturn(Optional.empty());

            CartException ex = assertThrows(CartException.class,
                    () -> service.mergeCart(userId, "invalid"));

            assertThat(ex.getErrorCode()).isEqualTo(CartErrorCode.GUEST_CART_NOT_FOUND);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // refreshPrices
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("refreshPrices")
    class RefreshPrices {

        @Test
        @DisplayName("refreshPrices_priceChanged_marksPriceChanged")
        void refreshPrices_priceChanged_marksPriceChanged() {
            Cart cart = buildCart(userId);
            CartItem item = buildCartItem(cart, productId, 2, new BigDecimal("10.00"));

            when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of(item));
            when(priceResolverService.resolve(eq(productId), isNull(), eq(2), eq(SupportedCurrency.GHS)))
                    .thenReturn(priceResult(new BigDecimal("15.00")));
            stubItemSave();
            stubCartSave();

            service.refreshPrices(userId);

            assertThat(item.getPriceChanged()).isTrue();
            assertThat(item.getUnitPrice()).isEqualByComparingTo("15.00");
            verify(cartItemRepository).save(item);
            verify(cartRepository).save(cart);
        }

        @Test
        @DisplayName("refreshPrices_priceResolutionFails_marksChanged")
        void refreshPrices_priceResolutionFails_marksChanged() {
            Cart cart = buildCart(userId);
            CartItem item = buildCartItem(cart, productId, 1, new BigDecimal("10.00"));

            when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of(item));
            when(priceResolverService.resolve(any(), any(), anyInt(), any()))
                    .thenThrow(new RuntimeException("pricing unavailable"));
            stubItemSave();
            stubCartSave();

            service.refreshPrices(userId);

            assertThat(item.getPriceChanged()).isTrue();
            verify(cartItemRepository).save(item);
            verify(cartRepository).save(cart);
        }

        @Test
        @DisplayName("refreshPrices_priceUnchanged_doesNotMarkChanged")
        void refreshPrices_priceUnchanged_doesNotMarkChanged() {
            Cart cart = buildCart(userId);
            CartItem item = buildCartItem(cart, productId, 2, new BigDecimal("10.00"));

            when(cartRepository.findByUserIdWithItems(userId)).thenReturn(Optional.of(cart));
            when(cartItemRepository.findByCartId(cart.getId())).thenReturn(List.of(item));
            when(priceResolverService.resolve(eq(productId), isNull(), eq(2), eq(SupportedCurrency.GHS)))
                    .thenReturn(priceResult(new BigDecimal("10.00")));
            stubItemSave();
            stubCartSave();

            service.refreshPrices(userId);

            assertThat(item.getPriceChanged()).isFalse();
        }
    }
}

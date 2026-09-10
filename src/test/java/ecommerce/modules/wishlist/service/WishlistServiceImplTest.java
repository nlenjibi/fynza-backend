package ecommerce.modules.wishlist.service;

import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.cart.dto.AddToCartRequest;
import ecommerce.modules.cart.service.CartService;
import ecommerce.modules.product.entity.Product;
import ecommerce.modules.product.repository.ProductRepository;
import ecommerce.modules.user.entity.User;
import ecommerce.modules.user.repository.UserRepository;
import ecommerce.modules.wishlist.dto.AddToWishlistRequest;
import ecommerce.modules.wishlist.dto.UpdateWishlistItemRequest;
import ecommerce.modules.wishlist.dto.WishlistItemDto;
import ecommerce.modules.wishlist.dto.WishlistSummaryDto;
import ecommerce.modules.wishlist.entity.WishlistItem;
import ecommerce.modules.wishlist.entity.WishlistPriority;
import ecommerce.modules.wishlist.repository.WishlistItemRepository;
import ecommerce.modules.wishlist.service.impl.WishlistServiceImpl;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WishlistServiceImpl Tests")
class WishlistServiceImplTest {

    @Mock private WishlistItemRepository wishlistItemRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CartService cartService;

    @InjectMocks
    private WishlistServiceImpl wishlistService;

    private User testUser;
    private Product testProduct;
    private WishlistItem testWishlistItem;
    private AddToWishlistRequest testAddRequest;
    private UUID userId;
    private UUID productId;
    private UUID wishlistItemId;

    @BeforeEach
    void setUp() {
        userId        = UUID.randomUUID();
        productId     = UUID.randomUUID();
        wishlistItemId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        testProduct = Product.builder()
                .name("Test Product")
                .slug("test-product")
                .sku("SKU-001")
                .isActive(true)
                .build();
        setField(testProduct, "id", productId);

        testWishlistItem = WishlistItem.builder()
                .user(testUser)
                .product(testProduct)
                .priority(WishlistPriority.HIGH)
                .notes("Test note")
                .desiredQuantity(1)
                .notifyOnPriceDrop(true)
                .notifyOnStock(false)
                .build();
        setField(testWishlistItem, "publicId", wishlistItemId);

        testAddRequest = AddToWishlistRequest.builder()
                .productId(productId)
                .priority(WishlistPriority.HIGH)
                .notes("Test note")
                .desiredQuantity(1)
                .notifyOnPriceDrop(true)
                .notifyOnStock(false)
                .targetPrice(BigDecimal.valueOf(79.99))
                .collectionName("My Collection")
                .build();
    }

    // ── addToWishlist ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addToWishlist")
    class AddToWishlistTests {

        @Test
        @DisplayName("Adds item when not already in wishlist")
        void addToWishlist_WhenValidRequest_AddsItem() {
            when(wishlistItemRepository.existsByUser_PublicIdAndProduct_Id(userId, productId)).thenReturn(false);
            when(userRepository.findByPublicId(userId)).thenReturn(Optional.of(testUser));
            when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
            when(wishlistItemRepository.save(any(WishlistItem.class))).thenReturn(testWishlistItem);

            WishlistItemDto result = wishlistService.addToWishlist(userId, testAddRequest);

            assertNotNull(result);
            assertEquals(wishlistItemId, result.getId());
            verify(wishlistItemRepository).save(any(WishlistItem.class));
        }

        @Test
        @DisplayName("Returns existing item when product already in wishlist")
        void addToWishlist_WhenProductExists_ReturnsExisting() {
            when(wishlistItemRepository.existsByUser_PublicIdAndProduct_Id(userId, productId)).thenReturn(true);
            when(wishlistItemRepository.findByUser_PublicIdAndProduct_Id(userId, productId))
                    .thenReturn(Optional.of(testWishlistItem));

            WishlistItemDto result = wishlistService.addToWishlist(userId, testAddRequest);

            assertNotNull(result);
            verify(wishlistItemRepository, never()).save(any(WishlistItem.class));
        }

        @Test
        @DisplayName("Throws exception when user not found")
        void addToWishlist_WhenUserNotFound_ThrowsException() {
            when(wishlistItemRepository.existsByUser_PublicIdAndProduct_Id(userId, productId)).thenReturn(false);
            when(userRepository.findByPublicId(userId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> wishlistService.addToWishlist(userId, testAddRequest));
        }

        @Test
        @DisplayName("Throws exception when product not found")
        void addToWishlist_WhenProductNotFound_ThrowsException() {
            when(wishlistItemRepository.existsByUser_PublicIdAndProduct_Id(userId, productId)).thenReturn(false);
            when(userRepository.findByPublicId(userId)).thenReturn(Optional.of(testUser));
            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> wishlistService.addToWishlist(userId, testAddRequest));
        }
    }

    // ── getUserWishlist ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserWishlist")
    class GetUserWishlistTests {

        @Test
        @DisplayName("Returns user wishlist items")
        void getUserWishlist_ReturnsWishlist() {
            when(wishlistItemRepository.findByUser_PublicIdOrderByCreatedAtDesc(userId))
                    .thenReturn(List.of(testWishlistItem));

            List<WishlistItemDto> result = wishlistService.getUserWishlist(userId);

            assertNotNull(result);
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Returns empty list when wishlist is empty")
        void getUserWishlist_WhenEmpty_ReturnsEmptyList() {
            when(wishlistItemRepository.findByUser_PublicIdOrderByCreatedAtDesc(userId))
                    .thenReturn(List.of());

            List<WishlistItemDto> result = wishlistService.getUserWishlist(userId);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ── removeFromWishlist ────────────────────────────────────────────────────

    @Nested
    @DisplayName("removeFromWishlist")
    class RemoveFromWishlistTests {

        @Test
        @DisplayName("Removes item when found")
        void removeFromWishlist_WhenItemExists_RemovesItem() {
            when(wishlistItemRepository.findByUser_PublicIdAndProduct_Id(userId, productId))
                    .thenReturn(Optional.of(testWishlistItem));
            doNothing().when(wishlistItemRepository).delete(testWishlistItem);

            wishlistService.removeFromWishlist(userId, productId);

            verify(wishlistItemRepository).delete(testWishlistItem);
        }

        @Test
        @DisplayName("Throws exception when item not found")
        void removeFromWishlist_WhenItemNotFound_ThrowsException() {
            when(wishlistItemRepository.findByUser_PublicIdAndProduct_Id(userId, productId))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> wishlistService.removeFromWishlist(userId, productId));
        }
    }

    // ── updateWishlistItem ────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateWishlistItem")
    class UpdateWishlistItemTests {

        @Test
        @DisplayName("Updates item fields and saves")
        void updateWishlistItem_WhenItemExists_UpdatesItem() {
            UpdateWishlistItemRequest updateRequest = UpdateWishlistItemRequest.builder()
                    .priority(WishlistPriority.LOW)
                    .notes("Updated note")
                    .desiredQuantity(2)
                    .notifyOnPriceDrop(false)
                    .notifyOnStock(true)
                    .build();

            when(wishlistItemRepository.findByUser_PublicIdAndProduct_Id(userId, productId))
                    .thenReturn(Optional.of(testWishlistItem));
            when(wishlistItemRepository.save(any(WishlistItem.class))).thenReturn(testWishlistItem);

            WishlistItemDto result = wishlistService.updateWishlistItem(userId, productId, updateRequest);

            assertNotNull(result);
            verify(wishlistItemRepository).save(any(WishlistItem.class));
        }

        @Test
        @DisplayName("Throws exception when item not found")
        void updateWishlistItem_WhenItemNotFound_ThrowsException() {
            when(wishlistItemRepository.findByUser_PublicIdAndProduct_Id(userId, productId))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> wishlistService.updateWishlistItem(userId, productId,
                            UpdateWishlistItemRequest.builder().build()));
        }
    }

    // ── moveToCart ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("moveToCart")
    class MoveToCartTests {

        @Test
        @DisplayName("Moves item to cart and removes from wishlist")
        void moveToCart_WhenItemExists_MovesToCart() {
            when(wishlistItemRepository.findByUser_PublicIdAndProduct_Id(userId, productId))
                    .thenReturn(Optional.of(testWishlistItem));
            doNothing().when(cartService).addItem(eq(userId), any(AddToCartRequest.class));
            doNothing().when(wishlistItemRepository).delete(testWishlistItem);

            wishlistService.moveToCart(userId, productId);

            verify(wishlistItemRepository).delete(testWishlistItem);
        }
    }

    // ── getWishlistSummary ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getWishlistSummary")
    class GetWishlistSummaryTests {

        @Test
        @DisplayName("Returns correct totals from repository stubs")
        void getWishlistSummary_ReturnsSummary() {
            Object[] totals = new Object[]{BigDecimal.valueOf(499.95), BigDecimal.valueOf(20.00)};
            when(wishlistItemRepository.findByUser_PublicIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());
            when(wishlistItemRepository.findTotalValueAndSavings(userId)).thenReturn(totals);

            WishlistSummaryDto result = wishlistService.getWishlistSummary(userId);

            assertNotNull(result);
            assertEquals(0, result.getTotalItems());
            assertEquals(BigDecimal.valueOf(499.95), result.getTotalValue());
            assertEquals(BigDecimal.valueOf(20.00), result.getTotalSavings());
        }
    }

    // ── clearWishlist ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("clearWishlist")
    class ClearWishlistTests {

        @Test
        @DisplayName("Delegates deletion to repository")
        void clearWishlist_ClearsAllItems() {
            when(wishlistItemRepository.deleteByUser_PublicId(userId)).thenReturn(1);

            wishlistService.clearWishlist(userId);

            verify(wishlistItemRepository).deleteByUser_PublicId(userId);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static void setField(Object target, String fieldName, Object value) {
        try {
            var f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

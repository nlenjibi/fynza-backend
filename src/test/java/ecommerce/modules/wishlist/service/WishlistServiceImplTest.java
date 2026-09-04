package ecommerce.modules.wishlist.service;

import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.cart.dto.AddToCartRequest;
import ecommerce.modules.cart.service.CartService;
import ecommerce.modules.product.repository.ProductRepository;
import ecommerce.modules.user.entity.User;
import ecommerce.modules.user.repository.UserRepository;
import ecommerce.modules.wishlist.dto.AddToWishlistRequest;
import ecommerce.modules.wishlist.dto.UpdateWishlistItemRequest;
import ecommerce.modules.wishlist.dto.WishlistItemDto;
import ecommerce.modules.wishlist.dto.WishlistSummaryDto;
import ecommerce.modules.wishlist.entity.WishlistItem;
import ecommerce.modules.wishlist.entity.WishlistPriority;
import ecommerce.modules.wishlist.mapper.WishlistMapper;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WishlistServiceImpl Tests")
class WishlistServiceImplTest {

    @Mock
    private WishlistItemRepository wishlistItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WishlistMapper wishlistMapper;

    @Mock
    private CartService cartService;

    @InjectMocks
    private WishlistServiceImpl wishlistService;

    private User testUser;
    private WishlistItem testWishlistItem;
    private WishlistItemDto testWishlistItemDto;
    private AddToWishlistRequest testAddRequest;
    private UUID userId;
    private UUID productId;
    private UUID wishlistItemId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
        wishlistItemId = UUID.randomUUID();
        
        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        testWishlistItem = WishlistItem.builder()
                .id(wishlistItemId)
                .user(testUser)
                .priority(WishlistPriority.HIGH)
                .notes("Test note")
                .desiredQuantity(1)
                .notifyOnPriceDrop(true)
                .notifyOnStock(false)
                .build();

        testWishlistItemDto = WishlistItemDto.builder()
                .id(wishlistItemId)
                .userId(userId)
                .product(WishlistItemDto.ProductSummary.builder()
                        .id(productId)
                        .name("Test Product")
                        .price(BigDecimal.valueOf(99.99))
                        .build())
                .priority(WishlistPriority.HIGH)
                .notes("Test note")
                .desiredQuantity(1)
                .notifyOnPriceDrop(true)
                .notifyOnStock(false)
                .build();

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

    @Nested
    @DisplayName("addToWishlist")
    class AddToWishlistTests {

        @Test
        @DisplayName("Should add item to wishlist successfully")
        void addToWishlist_WhenValidRequest_AddsItem() {
            when(wishlistItemRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(false);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(productRepository.findById(productId)).thenReturn(Optional.of(mock(ecommerce.modules.product.entity.Product.class)));
            when(wishlistItemRepository.save(any(WishlistItem.class))).thenReturn(testWishlistItem);
            when(wishlistMapper.toDto(any(WishlistItem.class))).thenReturn(testWishlistItemDto);

            WishlistItemDto result = wishlistService.addToWishlist(userId, testAddRequest);

            assertNotNull(result);
            assertEquals(wishlistItemId, result.getId());
            verify(wishlistItemRepository, times(1)).save(any(WishlistItem.class));
        }

        @Test
        @DisplayName("Should return existing item when product already in wishlist")
        void addToWishlist_WhenProductExists_ReturnsExisting() {
            when(wishlistItemRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(true);
            when(wishlistItemRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.of(testWishlistItem));
            when(wishlistMapper.toDto(any(WishlistItem.class))).thenReturn(testWishlistItemDto);

            WishlistItemDto result = wishlistService.addToWishlist(userId, testAddRequest);

            assertNotNull(result);
            verify(wishlistItemRepository, never()).save(any(WishlistItem.class));
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void addToWishlist_WhenUserNotFound_ThrowsException() {
            when(wishlistItemRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(false);
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> wishlistService.addToWishlist(userId, testAddRequest));
        }

        @Test
        @DisplayName("Should throw exception when product not found")
        void addToWishlist_WhenProductNotFound_ThrowsException() {
            when(wishlistItemRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(false);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> wishlistService.addToWishlist(userId, testAddRequest));
        }
    }

    @Nested
    @DisplayName("getUserWishlist")
    class GetUserWishlistTests {

        @Test
        @DisplayName("Should return user wishlist")
        void getUserWishlist_ReturnsWishlist() {
            List<WishlistItem> items = Arrays.asList(testWishlistItem);
            when(wishlistItemRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(items);
            when(wishlistMapper.toDto(any(WishlistItem.class))).thenReturn(testWishlistItemDto);

            List<WishlistItemDto> result = wishlistService.getUserWishlist(userId);

            assertNotNull(result);
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should return empty list when wishlist is empty")
        void getUserWishlist_WhenEmpty_ReturnsEmptyList() {
            when(wishlistItemRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());

            List<WishlistItemDto> result = wishlistService.getUserWishlist(userId);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("removeFromWishlist")
    class RemoveFromWishlistTests {

        @Test
        @DisplayName("Should remove item from wishlist")
        void removeFromWishlist_WhenItemExists_RemovesItem() {
            when(wishlistItemRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.of(testWishlistItem));
            doNothing().when(wishlistItemRepository).delete(testWishlistItem);

            wishlistService.removeFromWishlist(userId, productId);

            verify(wishlistItemRepository, times(1)).delete(testWishlistItem);
        }

        @Test
        @DisplayName("Should throw exception when item not found")
        void removeFromWishlist_WhenItemNotFound_ThrowsException() {
            when(wishlistItemRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> wishlistService.removeFromWishlist(userId, productId));
        }
    }

    @Nested
    @DisplayName("updateWishlistItem")
    class UpdateWishlistItemTests {

        @Test
        @DisplayName("Should update wishlist item")
        void updateWishlistItem_WhenItemExists_UpdatesItem() {
            UpdateWishlistItemRequest updateRequest = UpdateWishlistItemRequest.builder()
                    .priority(WishlistPriority.LOW)
                    .notes("Updated note")
                    .desiredQuantity(2)
                    .notifyOnPriceDrop(false)
                    .notifyOnStock(true)
                    .build();

            when(wishlistItemRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.of(testWishlistItem));
            when(wishlistItemRepository.save(any(WishlistItem.class))).thenReturn(testWishlistItem);
            when(wishlistMapper.toDto(any(WishlistItem.class))).thenReturn(testWishlistItemDto);

            WishlistItemDto result = wishlistService.updateWishlistItem(userId, productId, updateRequest);

            assertNotNull(result);
            verify(wishlistItemRepository, times(1)).save(any(WishlistItem.class));
        }

        @Test
        @DisplayName("Should throw exception when item not found")
        void updateWishlistItem_WhenItemNotFound_ThrowsException() {
            UpdateWishlistItemRequest updateRequest = UpdateWishlistItemRequest.builder()
                    .priority(WishlistPriority.LOW)
                    .build();

            when(wishlistItemRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> wishlistService.updateWishlistItem(userId, productId, updateRequest));
        }
    }

    @Nested
    @DisplayName("moveToCart")
    class MoveToCartTests {

        @Test
        @DisplayName("Should move item to cart")
        void moveToCart_WhenItemExists_MovesToCart() {
            when(wishlistItemRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.of(testWishlistItem));
            doNothing().when(cartService).addItem(eq(userId), any(AddToCartRequest.class));
            doNothing().when(wishlistItemRepository).delete(testWishlistItem);

            wishlistService.moveToCart(userId, productId);

            verify(wishlistItemRepository, times(1)).delete(testWishlistItem);
        }
    }

    @Nested
    @DisplayName("getWishlistSummary")
    class GetWishlistSummaryTests {

        @Test
        @DisplayName("Should return wishlist summary")
        void getWishlistSummary_ReturnsSummary() {
            WishlistSummaryDto summary = WishlistSummaryDto.builder()
                    .totalItems(5)
                    .totalValue(BigDecimal.valueOf(499.95))
                    .build();

            Object[] totals = new Object[]{BigDecimal.valueOf(499.95), BigDecimal.valueOf(20.00)};
            when(wishlistItemRepository.countByUserId(userId)).thenReturn(5L);
            when(wishlistItemRepository.findTotalValueAndSavings(userId)).thenReturn(totals);

            WishlistSummaryDto result = wishlistService.getWishlistSummary(userId);

            assertNotNull(result);
            assertEquals(5, result.getTotalItems());
            assertEquals(BigDecimal.valueOf(499.95), result.getTotalValue());
            assertEquals(BigDecimal.valueOf(20.00), result.getTotalSavings());
        }
    }

    @Nested
    @DisplayName("clearWishlist")
    class ClearWishlistTests {

        @Test
        @DisplayName("Should clear all wishlist items for user")
        void clearWishlist_ClearsAllItems() {
            when(wishlistItemRepository.deleteByUserId(userId)).thenReturn(1);

            wishlistService.clearWishlist(userId);

            verify(wishlistItemRepository, times(1)).deleteByUserId(userId);
        }
    }
}

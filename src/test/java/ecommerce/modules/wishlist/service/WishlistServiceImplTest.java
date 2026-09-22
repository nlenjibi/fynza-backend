package ecommerce.modules.wishlist.service;

import ecommerce.modules.cart.dto.AddToCartRequest;
import ecommerce.modules.cart.service.CartService;
import ecommerce.modules.wishlist.dto.request.AddWishlistItemRequest;
import ecommerce.modules.wishlist.dto.request.CreateWishlistRequest;
import ecommerce.modules.wishlist.dto.request.NotificationPreferenceRequest;
import ecommerce.modules.wishlist.dto.response.WishlistItemResponse;
import ecommerce.modules.wishlist.dto.response.WishlistResponse;
import ecommerce.modules.wishlist.entity.Wishlist;
import ecommerce.modules.wishlist.entity.WishlistItem;
import ecommerce.modules.wishlist.entity.WishlistStatus;
import ecommerce.modules.wishlist.entity.WishlistVisibility;
import ecommerce.modules.wishlist.exception.WishlistAccessDeniedException;
import ecommerce.modules.wishlist.exception.WishlistItemNotFoundException;
import ecommerce.modules.wishlist.exception.WishlistNotFoundException;
import ecommerce.modules.wishlist.repository.WishlistItemRepository;
import ecommerce.modules.wishlist.repository.WishlistRepository;
import ecommerce.modules.wishlist.service.impl.WishlistServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WishlistServiceImpl Tests")
class WishlistServiceImplTest {

    @Mock private WishlistRepository wishlistRepository;
    @Mock private WishlistItemRepository wishlistItemRepository;
    @Mock private WishlistPolicy wishlistPolicy;
    @Mock private CartService cartService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private WishlistServiceImpl wishlistService;

    private UUID customerId;
    private UUID wishlistPublicId;
    private UUID itemPublicId;
    private UUID productId;
    private Wishlist testWishlist;
    private WishlistItem testItem;

    @BeforeEach
    void setUp() {
        customerId      = UUID.randomUUID();
        wishlistPublicId = UUID.randomUUID();
        itemPublicId    = UUID.randomUUID();
        productId       = UUID.randomUUID();

        testWishlist = Wishlist.builder()
                .customerId(customerId)
                .name("Favourites")
                .status(WishlistStatus.ACTIVE)
                .visibility(WishlistVisibility.PRIVATE)
                .isDefault(true)
                .build();
        setField(testWishlist, "id", 1L);
        setField(testWishlist, "publicId", wishlistPublicId);
        setField(testWishlist, "createdAt", Instant.now());
        setField(testWishlist, "updatedAt", Instant.now());

        testItem = WishlistItem.builder()
                .wishlist(testWishlist)
                .productId(productId)
                .notifyOnPriceDrop(false)
                .notifyOnRestock(false)
                .build();
        setField(testItem, "publicId", itemPublicId);
        setField(testItem, "createdAt", Instant.now());
    }

    // ── createWishlist ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createWishlist")
    class CreateWishlistTests {

        @Test
        @DisplayName("Sets isDefault=true when customer has no other wishlists")
        void createWishlist_FirstWishlist_SetsDefault() {
            when(wishlistRepository.countByCustomerIdAndStatusNot(customerId, WishlistStatus.DELETED)).thenReturn(0L);
            when(wishlistRepository.save(any(Wishlist.class))).thenAnswer(inv -> {
                Wishlist w = inv.getArgument(0);
                setField(w, "publicId", wishlistPublicId);
                setField(w, "createdAt", Instant.now());
                setField(w, "updatedAt", Instant.now());
                return w;
            });
            when(wishlistItemRepository.findByWishlist_PublicId(any())).thenReturn(List.of());

            CreateWishlistRequest req = new CreateWishlistRequest();
            req.setName("Favourites");

            WishlistResponse result = wishlistService.createWishlist(customerId, req);

            assertNotNull(result);
            assertEquals("Favourites", result.getName());
            verify(wishlistRepository).save(argThat(w -> Boolean.TRUE.equals(w.getIsDefault())));
            verify(eventPublisher).publishEvent(any());
        }

        @Test
        @DisplayName("Does not set isDefault when customer already has wishlists")
        void createWishlist_NotFirst_DoesNotSetDefault() {
            when(wishlistRepository.countByCustomerIdAndStatusNot(customerId, WishlistStatus.DELETED)).thenReturn(2L);
            when(wishlistRepository.save(any(Wishlist.class))).thenAnswer(inv -> {
                Wishlist w = inv.getArgument(0);
                setField(w, "publicId", UUID.randomUUID());
                setField(w, "createdAt", Instant.now());
                setField(w, "updatedAt", Instant.now());
                return w;
            });
            when(wishlistItemRepository.findByWishlist_PublicId(any())).thenReturn(List.of());

            CreateWishlistRequest req = new CreateWishlistRequest();
            req.setName("Secondary");

            wishlistService.createWishlist(customerId, req);

            verify(wishlistRepository).save(argThat(w -> !Boolean.TRUE.equals(w.getIsDefault())));
        }
    }

    // ── getWishlist ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getWishlist")
    class GetWishlistTests {

        @Test
        @DisplayName("Returns wishlist when owner matches")
        void getWishlist_ValidOwner_Returns() {
            when(wishlistRepository.findByPublicId(wishlistPublicId)).thenReturn(Optional.of(testWishlist));
            doNothing().when(wishlistPolicy).assertOwner(customerId, testWishlist);
            when(wishlistItemRepository.findByWishlist_PublicId(wishlistPublicId)).thenReturn(List.of());

            WishlistResponse result = wishlistService.getWishlist(customerId, wishlistPublicId);

            assertNotNull(result);
            assertEquals(wishlistPublicId, result.getId());
        }

        @Test
        @DisplayName("Throws WishlistNotFoundException when not found")
        void getWishlist_NotFound_Throws() {
            when(wishlistRepository.findByPublicId(wishlistPublicId)).thenReturn(Optional.empty());

            assertThrows(WishlistNotFoundException.class,
                    () -> wishlistService.getWishlist(customerId, wishlistPublicId));
        }

        @Test
        @DisplayName("Throws WishlistAccessDeniedException when not owner")
        void getWishlist_WrongOwner_Throws() {
            when(wishlistRepository.findByPublicId(wishlistPublicId)).thenReturn(Optional.of(testWishlist));
            doThrow(new WishlistAccessDeniedException()).when(wishlistPolicy).assertOwner(any(), eq(testWishlist));

            assertThrows(WishlistAccessDeniedException.class,
                    () -> wishlistService.getWishlist(UUID.randomUUID(), wishlistPublicId));
        }
    }

    // ── addItem ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addItem")
    class AddItemTests {

        @Test
        @DisplayName("Saves new item when not duplicate")
        void addItem_NewItem_Saves() {
            when(wishlistRepository.findByPublicId(wishlistPublicId)).thenReturn(Optional.of(testWishlist));
            doNothing().when(wishlistPolicy).assertOwner(customerId, testWishlist);
            when(wishlistItemRepository.existsByWishlist_PublicIdAndProductIdAndVariantId(
                    wishlistPublicId, productId, null)).thenReturn(false);
            when(wishlistItemRepository.save(any(WishlistItem.class))).thenAnswer(inv -> {
                WishlistItem item = inv.getArgument(0);
                setField(item, "publicId", itemPublicId);
                setField(item, "createdAt", Instant.now());
                return item;
            });

            AddWishlistItemRequest req = new AddWishlistItemRequest();
            req.setProductId(productId);

            WishlistItemResponse result = wishlistService.addItem(customerId, wishlistPublicId, req);

            assertNotNull(result);
            assertEquals(productId, result.getProductId());
            verify(wishlistItemRepository).save(any(WishlistItem.class));
            verify(eventPublisher).publishEvent(any());
        }

        @Test
        @DisplayName("Returns existing item idempotently on duplicate")
        void addItem_Duplicate_ReturnsExisting() {
            when(wishlistRepository.findByPublicId(wishlistPublicId)).thenReturn(Optional.of(testWishlist));
            doNothing().when(wishlistPolicy).assertOwner(customerId, testWishlist);
            when(wishlistItemRepository.existsByWishlist_PublicIdAndProductIdAndVariantId(
                    wishlistPublicId, productId, null)).thenReturn(true);
            when(wishlistItemRepository.findByWishlist_PublicIdAndProductIdAndVariantId(
                    wishlistPublicId, productId, null)).thenReturn(Optional.of(testItem));

            AddWishlistItemRequest req = new AddWishlistItemRequest();
            req.setProductId(productId);

            WishlistItemResponse result = wishlistService.addItem(customerId, wishlistPublicId, req);

            assertNotNull(result);
            verify(wishlistItemRepository, never()).save(any());
        }
    }

    // ── removeItem ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("removeItem")
    class RemoveItemTests {

        @Test
        @DisplayName("Deletes item when owner matches")
        void removeItem_ValidOwner_Deletes() {
            when(wishlistItemRepository.findByPublicId(itemPublicId)).thenReturn(Optional.of(testItem));
            doNothing().when(wishlistPolicy).assertItemOwner(customerId, testItem);

            wishlistService.removeItem(customerId, itemPublicId);

            verify(wishlistItemRepository).delete(testItem);
            verify(eventPublisher).publishEvent(any());
        }

        @Test
        @DisplayName("Throws WishlistItemNotFoundException when not found")
        void removeItem_NotFound_Throws() {
            when(wishlistItemRepository.findByPublicId(itemPublicId)).thenReturn(Optional.empty());

            assertThrows(WishlistItemNotFoundException.class,
                    () -> wishlistService.removeItem(customerId, itemPublicId));
        }
    }

    // ── moveToCart ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("moveToCart")
    class MoveToCartTests {

        @Test
        @DisplayName("Calls CartService.addItem and deletes item from wishlist")
        void moveToCart_ValidOwner_MovesAndDeletes() {
            when(wishlistItemRepository.findByPublicId(itemPublicId)).thenReturn(Optional.of(testItem));
            doNothing().when(wishlistPolicy).assertItemOwner(customerId, testItem);
            when(cartService.addItem(eq(customerId), any(AddToCartRequest.class))).thenReturn(null);

            wishlistService.moveToCart(customerId, itemPublicId);

            verify(cartService).addItem(eq(customerId), any(AddToCartRequest.class));
            verify(wishlistItemRepository).delete(testItem);
            verify(eventPublisher).publishEvent(any());
        }
    }

    // ── updateNotificationPreferences ─────────────────────────────────────────

    @Nested
    @DisplayName("updateNotificationPreferences")
    class UpdateNotificationsTests {

        @Test
        @DisplayName("Updates and saves notification flags")
        void updateNotifications_Saves() {
            when(wishlistItemRepository.findByPublicId(itemPublicId)).thenReturn(Optional.of(testItem));
            doNothing().when(wishlistPolicy).assertItemOwner(customerId, testItem);
            when(wishlistItemRepository.save(testItem)).thenReturn(testItem);

            NotificationPreferenceRequest req = new NotificationPreferenceRequest();
            req.setNotifyOnPriceDrop(true);
            req.setNotifyOnRestock(true);

            WishlistItemResponse result = wishlistService.updateNotificationPreferences(customerId, itemPublicId, req);

            assertNotNull(result);
            assertTrue(testItem.getNotifyOnPriceDrop());
            assertTrue(testItem.getNotifyOnRestock());
            verify(wishlistItemRepository).save(testItem);
        }
    }

    // ── getSharedWishlist ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("getSharedWishlist")
    class SharedWishlistTests {

        @Test
        @DisplayName("Returns SHARED wishlist for valid token hash")
        void getSharedWishlist_Shared_Returns() {
            testWishlist.setVisibility(WishlistVisibility.SHARED);
            when(wishlistRepository.findByShareTokenHash(anyString())).thenReturn(Optional.of(testWishlist));
            when(wishlistItemRepository.findByWishlist_PublicId(wishlistPublicId)).thenReturn(List.of());

            WishlistResponse result = wishlistService.getSharedWishlist("sometoken");

            assertNotNull(result);
            assertEquals(WishlistVisibility.SHARED, result.getVisibility());
        }

        @Test
        @DisplayName("Throws WishlistNotFoundException when token not found")
        void getSharedWishlist_InvalidToken_Throws() {
            when(wishlistRepository.findByShareTokenHash(anyString())).thenReturn(Optional.empty());

            assertThrows(WishlistNotFoundException.class,
                    () -> wishlistService.getSharedWishlist("bad-token"));
        }

        @Test
        @DisplayName("Throws WishlistNotFoundException when wishlist is PRIVATE")
        void getSharedWishlist_PrivateWishlist_Throws() {
            testWishlist.setVisibility(WishlistVisibility.PRIVATE);
            when(wishlistRepository.findByShareTokenHash(anyString())).thenReturn(Optional.of(testWishlist));

            assertThrows(WishlistNotFoundException.class,
                    () -> wishlistService.getSharedWishlist("sometoken"));
        }
    }

    // ── isInWishlist ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isInWishlist")
    class IsInWishlistTests {

        @Test
        @DisplayName("Returns true when item exists for customer")
        void isInWishlist_Exists_ReturnsTrue() {
            when(wishlistItemRepository.existsByWishlist_CustomerIdAndProductIdAndVariantId(
                    customerId, productId, null)).thenReturn(true);

            assertTrue(wishlistService.isInWishlist(customerId, productId, null));
        }

        @Test
        @DisplayName("Returns false when item does not exist for customer")
        void isInWishlist_NotExists_ReturnsFalse() {
            when(wishlistItemRepository.existsByWishlist_CustomerIdAndProductIdAndVariantId(
                    customerId, productId, null)).thenReturn(false);

            assertFalse(wishlistService.isInWishlist(customerId, productId, null));
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Class<?> cls = target.getClass();
            while (cls != null) {
                try {
                    var f = cls.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    f.set(target, value);
                    return;
                } catch (NoSuchFieldException e) {
                    cls = cls.getSuperclass();
                }
            }
            throw new NoSuchFieldException(fieldName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

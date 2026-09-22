package ecommerce.modules.wishlist.service.impl;

import ecommerce.modules.cart.dto.AddToCartRequest;
import ecommerce.modules.cart.service.CartService;
import ecommerce.modules.wishlist.dto.request.*;
import ecommerce.modules.wishlist.dto.response.*;
import ecommerce.modules.wishlist.entity.Wishlist;
import ecommerce.modules.wishlist.entity.WishlistItem;
import ecommerce.modules.wishlist.entity.WishlistStatus;
import ecommerce.modules.wishlist.entity.WishlistVisibility;
import ecommerce.modules.wishlist.event.*;
import ecommerce.modules.wishlist.exception.*;
import ecommerce.modules.wishlist.entity.WishlistSummaryView;
import ecommerce.modules.wishlist.repository.WishlistItemRepository;
import ecommerce.modules.wishlist.repository.WishlistRepository;
import ecommerce.modules.wishlist.repository.WishlistSummaryViewRepository;
import ecommerce.modules.wishlist.service.WishlistPolicy;
import ecommerce.modules.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final WishlistSummaryViewRepository wishlistSummaryViewRepository;
    private final WishlistPolicy wishlistPolicy;
    private final CartService cartService;
    private final ApplicationEventPublisher eventPublisher;

    // =========================================================================
    // Wishlist CRUD
    // =========================================================================

    @Override
    @Transactional
    public WishlistResponse createWishlist(UUID customerId, CreateWishlistRequest request) {
        boolean isFirst = wishlistRepository.countByCustomerIdAndStatusNot(customerId, WishlistStatus.DELETED) == 0;

        Wishlist wishlist = Wishlist.builder()
                .customerId(customerId)
                .name(request.getName())
                .description(request.getDescription())
                .visibility(request.getVisibility() != null ? request.getVisibility() : WishlistVisibility.PRIVATE)
                .isDefault(isFirst)
                .build();

        wishlist = wishlistRepository.save(wishlist);
        eventPublisher.publishEvent(new WishlistCreatedEvent(wishlist.getPublicId(), customerId, wishlist.getName()));
        log.info("Created wishlist id={} for customer={}", wishlist.getPublicId(), customerId);
        return toResponse(wishlist);
    }

    @Override
    public WishlistResponse getWishlist(UUID customerId, UUID wishlistPublicId) {
        Wishlist wishlist = findByPublicIdOrThrow(wishlistPublicId);
        wishlistPolicy.assertOwner(customerId, wishlist);
        return toResponse(wishlist);
    }

    @Override
    @Transactional
    public WishlistResponse updateWishlist(UUID customerId, UUID wishlistPublicId, UpdateWishlistRequest request) {
        Wishlist wishlist = findByPublicIdOrThrow(wishlistPublicId);
        wishlistPolicy.assertOwner(customerId, wishlist);

        if (request.getName() != null) wishlist.setName(request.getName());
        if (request.getDescription() != null) wishlist.setDescription(request.getDescription());
        if (request.getVisibility() != null) wishlist.setVisibility(request.getVisibility());

        return toResponse(wishlistRepository.save(wishlist));
    }

    @Override
    @Transactional
    public void deleteWishlist(UUID customerId, UUID wishlistPublicId) {
        Wishlist wishlist = findByPublicIdOrThrow(wishlistPublicId);
        wishlistPolicy.assertOwner(customerId, wishlist);

        if (Boolean.TRUE.equals(wishlist.getIsDefault())) {
            long remaining = wishlistRepository.countByCustomerIdAndStatusNot(customerId, WishlistStatus.DELETED);
            if (remaining > 1) {
                throw new WishlistNotFoundException(
                        "Cannot delete the default wishlist while others exist. Set a new default first.");
            }
        }

        wishlist.setStatus(WishlistStatus.DELETED);
        wishlistRepository.save(wishlist);
        log.info("Soft-deleted wishlist id={} for customer={}", wishlistPublicId, customerId);
    }

    @Override
    @Transactional
    public WishlistResponse getOrCreateDefaultWishlist(UUID customerId) {
        return wishlistRepository.findByCustomerIdAndIsDefaultTrue(customerId)
                .map(this::toResponse)
                .orElseGet(() -> {
                    CreateWishlistRequest req = new CreateWishlistRequest();
                    req.setName("My Wishlist");
                    return createWishlist(customerId, req);
                });
    }

    @Override
    public List<WishlistResponse> getMyWishlists(UUID customerId) {
        return wishlistSummaryViewRepository
                .findByCustomerIdAndStatusNot(customerId, WishlistStatus.DELETED.name())
                .stream().map(this::toSummaryResponse).toList();
    }

    // =========================================================================
    // Item operations
    // =========================================================================

    @Override
    @Transactional
    public WishlistItemResponse addItem(UUID customerId, UUID wishlistPublicId, AddWishlistItemRequest request) {
        Wishlist wishlist = findByPublicIdOrThrow(wishlistPublicId);
        wishlistPolicy.assertOwner(customerId, wishlist);

        if (wishlistItemRepository.existsByWishlist_PublicIdAndProductIdAndVariantId(
                wishlistPublicId, request.getProductId(), request.getVariantId())) {
            return wishlistItemRepository
                    .findByWishlist_PublicIdAndProductIdAndVariantId(
                            wishlistPublicId, request.getProductId(), request.getVariantId())
                    .map(this::toItemResponse)
                    .orElseThrow();
        }

        WishlistItem item = WishlistItem.builder()
                .wishlist(wishlist)
                .productId(request.getProductId())
                .variantId(request.getVariantId())
                .notifyOnPriceDrop(Boolean.TRUE.equals(request.getNotifyOnPriceDrop()))
                .notifyOnRestock(Boolean.TRUE.equals(request.getNotifyOnRestock()))
                .build();

        item = wishlistItemRepository.save(item);
        eventPublisher.publishEvent(new WishlistItemAddedEvent(
                wishlist.getPublicId(), customerId, item.getProductId(), item.getVariantId()));
        return toItemResponse(item);
    }

    @Override
    @Transactional
    public void removeItem(UUID customerId, UUID itemPublicId) {
        WishlistItem item = findItemByPublicIdOrThrow(itemPublicId);
        wishlistPolicy.assertItemOwner(customerId, item);
        eventPublisher.publishEvent(new WishlistItemRemovedEvent(
                item.getWishlist().getPublicId(), customerId, item.getPublicId()));
        wishlistItemRepository.delete(item);
    }

    @Override
    @Transactional
    public void moveToCart(UUID customerId, UUID itemPublicId) {
        WishlistItem item = findItemByPublicIdOrThrow(itemPublicId);
        wishlistPolicy.assertItemOwner(customerId, item);

        cartService.addItem(customerId, AddToCartRequest.builder()
                .productId(item.getProductId())
                .variantId(item.getVariantId())
                .quantity(1)
                .build());

        eventPublisher.publishEvent(new WishlistItemMovedToCartEvent(
                item.getWishlist().getPublicId(), customerId, item.getProductId(), item.getVariantId()));
        wishlistItemRepository.delete(item);
    }

    @Override
    @Transactional
    public void addToCart(UUID customerId, UUID itemPublicId) {
        WishlistItem item = findItemByPublicIdOrThrow(itemPublicId);
        wishlistPolicy.assertItemOwner(customerId, item);

        cartService.addItem(customerId, AddToCartRequest.builder()
                .productId(item.getProductId())
                .variantId(item.getVariantId())
                .quantity(1)
                .build());
    }

    @Override
    @Transactional
    public WishlistItemResponse updateNotificationPreferences(UUID customerId, UUID itemPublicId,
                                                               NotificationPreferenceRequest request) {
        WishlistItem item = findItemByPublicIdOrThrow(itemPublicId);
        wishlistPolicy.assertItemOwner(customerId, item);

        if (request.getNotifyOnPriceDrop() != null) item.setNotifyOnPriceDrop(request.getNotifyOnPriceDrop());
        if (request.getNotifyOnRestock() != null) item.setNotifyOnRestock(request.getNotifyOnRestock());

        return toItemResponse(wishlistItemRepository.save(item));
    }

    @Override
    public boolean isInWishlist(UUID customerId, UUID productId, UUID variantId) {
        return wishlistItemRepository.existsByWishlist_CustomerIdAndProductIdAndVariantId(
                customerId, productId, variantId);
    }

    // =========================================================================
    // Guest wishlist
    // =========================================================================

    @Override
    @Transactional
    public GuestWishlistResponse createGuestWishlist() {
        String rawToken = generateSecureToken();
        String tokenHash = sha256(rawToken);

        Wishlist wishlist = Wishlist.builder()
                .guestTokenHash(tokenHash)
                .name("Guest Wishlist")
                .build();

        wishlist = wishlistRepository.save(wishlist);
        log.info("Created guest wishlist id={}", wishlist.getPublicId());
        return GuestWishlistResponse.builder()
                .wishlistId(wishlist.getPublicId())
                .guestToken(rawToken)
                .build();
    }

    @Override
    @Transactional
    public void mergeGuestWishlist(UUID customerId, String guestToken) {
        String tokenHash = sha256(guestToken);
        Wishlist guestWishlist = wishlistRepository.findByGuestTokenHash(tokenHash)
                .orElseThrow(() -> new WishlistNotFoundException("Guest wishlist not found"));

        Wishlist defaultWishlist = wishlistRepository.findByCustomerIdAndIsDefaultTrue(customerId)
                .orElseGet(() -> {
                    Wishlist w = Wishlist.builder()
                            .customerId(customerId)
                            .name("My Wishlist")
                            .isDefault(true)
                            .build();
                    return wishlistRepository.save(w);
                });

        List<WishlistItem> guestItems = wishlistItemRepository.findByWishlist_PublicId(guestWishlist.getPublicId());
        int mergedCount = 0;

        for (WishlistItem guestItem : guestItems) {
            boolean alreadyExists = wishlistItemRepository.existsByWishlist_PublicIdAndProductIdAndVariantId(
                    defaultWishlist.getPublicId(), guestItem.getProductId(), guestItem.getVariantId());
            if (!alreadyExists) {
                WishlistItem newItem = WishlistItem.builder()
                        .wishlist(defaultWishlist)
                        .productId(guestItem.getProductId())
                        .variantId(guestItem.getVariantId())
                        .notifyOnPriceDrop(guestItem.getNotifyOnPriceDrop())
                        .notifyOnRestock(guestItem.getNotifyOnRestock())
                        .build();
                wishlistItemRepository.save(newItem);
                mergedCount++;
            }
        }

        guestWishlist.setStatus(WishlistStatus.ARCHIVED);
        wishlistRepository.save(guestWishlist);

        eventPublisher.publishEvent(new WishlistMergedEvent(customerId, mergedCount, guestWishlist.getPublicId()));
        log.info("Merged {} items from guest wishlist into customer={} wishlist", mergedCount, customerId);
    }

    // =========================================================================
    // Sharing
    // =========================================================================

    @Override
    @Transactional
    public String shareWishlist(UUID customerId, UUID wishlistPublicId) {
        Wishlist wishlist = findByPublicIdOrThrow(wishlistPublicId);
        wishlistPolicy.assertOwner(customerId, wishlist);

        String rawToken = generateSecureToken();
        wishlist.setShareTokenHash(sha256(rawToken));
        wishlist.setVisibility(WishlistVisibility.SHARED);
        wishlistRepository.save(wishlist);

        eventPublisher.publishEvent(new WishlistSharedEvent(wishlist.getPublicId(), customerId));
        return rawToken;
    }

    @Override
    @Transactional
    public void unshareWishlist(UUID customerId, UUID wishlistPublicId) {
        Wishlist wishlist = findByPublicIdOrThrow(wishlistPublicId);
        wishlistPolicy.assertOwner(customerId, wishlist);

        wishlist.setShareTokenHash(null);
        wishlist.setVisibility(WishlistVisibility.PRIVATE);
        wishlistRepository.save(wishlist);
    }

    @Override
    @Transactional
    public String regenerateShareToken(UUID customerId, UUID wishlistPublicId) {
        Wishlist wishlist = findByPublicIdOrThrow(wishlistPublicId);
        wishlistPolicy.assertOwner(customerId, wishlist);

        String rawToken = generateSecureToken();
        wishlist.setShareTokenHash(sha256(rawToken));
        wishlistRepository.save(wishlist);
        return rawToken;
    }

    @Override
    public WishlistResponse getSharedWishlist(String shareToken) {
        String tokenHash = sha256(shareToken);
        Wishlist wishlist = wishlistRepository.findByShareTokenHash(tokenHash)
                .orElseThrow(() -> new WishlistNotFoundException("Shared wishlist not found"));

        if (wishlist.getVisibility() != WishlistVisibility.SHARED
                && wishlist.getVisibility() != WishlistVisibility.PUBLIC) {
            throw new WishlistNotFoundException("Shared wishlist not found");
        }
        return toResponse(wishlist);
    }

    // =========================================================================
    // Mapping
    // =========================================================================

    private WishlistResponse toSummaryResponse(WishlistSummaryView view) {
        return WishlistResponse.builder()
                .id(view.getPublicId())
                .name(view.getName())
                .description(view.getDescription())
                .status(WishlistStatus.valueOf(view.getStatus()))
                .visibility(WishlistVisibility.valueOf(view.getVisibility()))
                .isDefault(view.getIsDefault())
                .itemCount(view.getItemCount() != null ? view.getItemCount() : 0)
                .items(List.of())
                .createdAt(view.getCreatedAt())
                .updatedAt(view.getUpdatedAt())
                .build();
    }

    private WishlistResponse toResponse(Wishlist wishlist) {
        List<WishlistItem> items = wishlistItemRepository.findByWishlist_PublicId(wishlist.getPublicId());
        List<WishlistItemResponse> itemResponses = items.stream().map(this::toItemResponse).toList();
        return WishlistResponse.builder()
                .id(wishlist.getPublicId())
                .name(wishlist.getName())
                .description(wishlist.getDescription())
                .status(wishlist.getStatus())
                .visibility(wishlist.getVisibility())
                .isDefault(wishlist.getIsDefault())
                .itemCount(itemResponses.size())
                .items(itemResponses)
                .createdAt(wishlist.getCreatedAt())
                .updatedAt(wishlist.getUpdatedAt())
                .build();
    }

    private WishlistItemResponse toItemResponse(WishlistItem item) {
        return WishlistItemResponse.builder()
                .id(item.getPublicId())
                .wishlistId(item.getWishlist().getPublicId())
                .productId(item.getProductId())
                .variantId(item.getVariantId())
                .notifyOnPriceDrop(item.getNotifyOnPriceDrop())
                .notifyOnRestock(item.getNotifyOnRestock())
                .createdAt(item.getCreatedAt())
                .build();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Wishlist findByPublicIdOrThrow(UUID publicId) {
        return wishlistRepository.findByPublicId(publicId)
                .orElseThrow(() -> new WishlistNotFoundException("Wishlist not found: " + publicId));
    }

    private WishlistItem findItemByPublicIdOrThrow(UUID publicId) {
        return wishlistItemRepository.findByPublicId(publicId)
                .orElseThrow(() -> new WishlistItemNotFoundException("Wishlist item not found: " + publicId));
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

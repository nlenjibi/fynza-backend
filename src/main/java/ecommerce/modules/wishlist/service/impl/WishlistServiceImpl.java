package ecommerce.modules.wishlist.service.impl;

import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.cart.dto.AddToCartRequest;
import ecommerce.modules.cart.service.CartService;
import ecommerce.modules.product.repository.ProductRepository;
import ecommerce.modules.user.repository.UserRepository;
import ecommerce.modules.wishlist.dto.AddToWishlistRequest;
import ecommerce.modules.wishlist.dto.UpdateWishlistItemRequest;
import ecommerce.modules.wishlist.dto.WishlistItemDto;
import ecommerce.modules.wishlist.dto.WishlistSummaryDto;
import ecommerce.modules.wishlist.entity.WishlistItem;
import ecommerce.modules.wishlist.entity.WishlistPriority;
import ecommerce.modules.wishlist.mapper.WishlistMapper;
import ecommerce.modules.wishlist.repository.WishlistItemRepository;
import ecommerce.modules.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WishlistServiceImpl implements WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final WishlistMapper wishlistMapper;
    private final CartService cartService;

    @Override
    @Transactional
    @CacheEvict(value = {
            "wishlists", "wishlists-paginated", "wishlists-summary", "wishlists-check"
    }, allEntries = true)
    public WishlistItemDto addToWishlist(UUID userId, AddToWishlistRequest request) {
        log.debug("addToWishlist: userId={}, productId={}", userId, request.getProductId());

        if (wishlistItemRepository.existsByUserIdAndProductId(userId, request.getProductId())) {
            return wishlistItemRepository
                    .findByUserIdAndProductId(userId, request.getProductId())
                    .map(wishlistMapper::toDto)
                    .orElseThrow();
        }

        var user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("User", userId));
        var product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> ResourceNotFoundException.forResource("Product", request.getProductId()));

        WishlistItem item = WishlistItem.builder()
                .user(user)
                .product(product)
                .priority(request.getPriority() != null ? request.getPriority() : WishlistPriority.MEDIUM)
                .notes(request.getNotes())
                .desiredQuantity(request.getDesiredQuantity() != null ? request.getDesiredQuantity() : 1)
                .notifyOnPriceDrop(Boolean.TRUE.equals(request.getNotifyOnPriceDrop()))
                .notifyOnStock(Boolean.TRUE.equals(request.getNotifyOnStock()))
                .targetPrice(request.getTargetPrice())
                .collectionName(request.getCollectionName())
                .build();

        return wishlistMapper.toDto(wishlistItemRepository.save(item));
    }

    @Override
    @Cacheable(value = "wishlists", key = "#userId")
    public List<WishlistItemDto> getUserWishlist(UUID userId) {
        log.debug("getUserWishlist: userId={}", userId);
        return wishlistItemRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(wishlistMapper::toDto).toList();
    }

    @Override
    @Cacheable(value = "wishlists-paginated", key = "#userId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<WishlistItemDto> getUserWishlistPaginated(UUID userId, Pageable pageable) {
        log.debug("getUserWishlistPaginated: userId={}, page={}", userId, pageable.getPageNumber());
        return wishlistItemRepository.findByUserId(userId, pageable).map(wishlistMapper::toDto);
    }

    @Override
    @Cacheable(value = "wishlists-summary", key = "#userId")
    public WishlistSummaryDto getWishlistSummary(UUID userId) {
        log.debug("getWishlistSummary: userId={}", userId);

        List<WishlistItem> items = wishlistItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Object[] totals = wishlistItemRepository.findTotalValueAndSavings(userId);
        BigDecimal totalValue = totals[0] != null ? (BigDecimal) totals[0] : BigDecimal.ZERO;
        BigDecimal totalSavings = totals[1] != null ? (BigDecimal) totals[1] : BigDecimal.ZERO;

        long inStock = items.stream().filter(i -> i.getProduct().isInStock()).count();
        long priceDrops = items.stream().filter(WishlistItem::isPriceDropped).count();
        long purchased = items.stream().filter(i -> Boolean.TRUE.equals(i.getPurchased())).count();

        return WishlistSummaryDto.builder()
                .userId(userId)
                .totalItems(items.size())
                .inStockItems((int) inStock)
                .outOfStockItems((int) (items.size() - inStock))
                .itemsWithPriceDrops((int) priceDrops)
                .purchasedItems((int) purchased)
                .totalValue(totalValue)
                .totalSavings(totalSavings)
                .items(items.stream().map(wishlistMapper::toDto).toList())
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            "wishlists", "wishlists-paginated", "wishlists-summary", "wishlists-check"
    }, allEntries = true)
    public void removeFromWishlist(UUID userId, UUID productId) {
        log.debug("removeFromWishlist: userId={}, productId={}", userId, productId);
        WishlistItem item = wishlistItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("WishlistItem", productId));
        wishlistItemRepository.delete(item);
    }

    @Override
    @Transactional
    @CachePut(value = "wishlists", key = "#userId")
    @CacheEvict(value = {
            "wishlists-paginated", "wishlists-summary", "wishlists-check"
    }, allEntries = true)
    public WishlistItemDto updateWishlistItem(UUID userId, UUID productId, UpdateWishlistItemRequest request) {
        log.debug("updateWishlistItem: userId={}, productId={}", userId, productId);
        WishlistItem item = wishlistItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("WishlistItem", productId));

        if (request.getNotes() != null) {
            item.setNotes(request.getNotes());
        }
        if (request.getPriority() != null) {
            item.setPriority(request.getPriority());
        }
        if (request.getDesiredQuantity() != null) {
            item.setDesiredQuantity(request.getDesiredQuantity());
        }
        if (request.getTargetPrice() != null) {
            item.setTargetPrice(request.getTargetPrice());
        }
        if (request.getNotifyOnPriceDrop() != null) {
            item.setNotifyOnPriceDrop(request.getNotifyOnPriceDrop());
        }
        if (request.getNotifyOnStock() != null) {
            item.setNotifyOnStock(request.getNotifyOnStock());
        }
        if (request.getIsPublic() != null) {
            item.setIsPublic(request.getIsPublic());
        }

        return wishlistMapper.toDto(wishlistItemRepository.save(item));
    }

    @Override
    @Cacheable(value = "wishlists-check", key = "#userId + ':' + #productId")
    public boolean isInWishlist(UUID userId, UUID productId) {
        return wishlistItemRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            "wishlists", "wishlists-paginated", "wishlists-summary",
            "wishlists-check", "wishlists-drops", "wishlists-analytics"
    }, allEntries = true)
    public void clearWishlist(UUID userId) {
        log.debug("clearWishlist: userId={}", userId);
        int deleted = wishlistItemRepository.deleteByUserId(userId);
        log.info("clearWishlist: removed {} items for userId={}", deleted, userId);
    }

    @Override
    @Cacheable(value = "wishlists-drops", key = "#userId")
    public List<WishlistItemDto> getItemsWithPriceDrops(UUID userId) {
        return wishlistItemRepository.findItemsWithPriceDrops(userId)
                .stream().map(wishlistMapper::toDto).toList();
    }

    @Override
    @Transactional
    @CachePut(value = "wishlists", key = "#userId")
    @CacheEvict(value = {"wishlists-summary", "wishlists-analytics"}, allEntries = true)
    public WishlistItemDto markAsPurchased(UUID userId, UUID productId) {
        WishlistItem item = wishlistItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("WishlistItem", productId));
        item.markAsPurchased();
        return wishlistMapper.toDto(wishlistItemRepository.save(item));
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            "wishlists", "wishlists-paginated", "wishlists-summary",
            "wishlists-check", "wishlists-drops", "wishlists-analytics"
    }, allEntries = true)
    public void moveToCart(UUID userId, UUID productId) {
        WishlistItem item = wishlistItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("WishlistItem", productId));
        
        log.debug("moveToCart: productId={} for userId={}", productId, userId);

        AddToCartRequest cartRequest = AddToCartRequest.builder()
                .productId(item.getProduct().getId())
                .quantity(item.getDesiredQuantity() != null ? item.getDesiredQuantity() : 1)
                .build();

        cartService.addItem(userId, cartRequest);
        
        wishlistItemRepository.delete(item);
        log.info("Moved wishlist item to cart and removed from wishlist: productId={}, userId={}", productId, userId);
    }

    @Override
    public long getWishlistCount(UUID userId) {
        return wishlistItemRepository.countByUserId(userId);
    }
}

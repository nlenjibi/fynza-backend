package ecommerce.graphql.resolver.wishlist;

import ecommerce.common.response.PaginatedResponse;
import ecommerce.graphql.dto.WishlistItemPage;
import ecommerce.graphql.input.AddToWishlistInput;
import ecommerce.graphql.input.PageInput;
import ecommerce.graphql.input.SortDirection;
import ecommerce.graphql.input.UpdateWishlistItemInput;
import ecommerce.modules.wishlist.dto.AddToWishlistRequest;
import ecommerce.modules.wishlist.dto.UpdateWishlistItemRequest;
import ecommerce.modules.wishlist.dto.WishlistItemDto;
import ecommerce.modules.wishlist.dto.WishlistSummaryDto;
import ecommerce.modules.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('CUSTOMER')")
public class WishlistResolver {

    private final WishlistService wishlistService;

    // =========================================================================
    // QUERIES
    // =========================================================================

    @QueryMapping
    public List<WishlistItemDto> myWishlist(@ContextValue UUID userId) {
        log.info("GQL myWishlist(user={})", userId);
        return wishlistService.getUserWishlist(userId);
    }

    @QueryMapping
    public WishlistItemPage myWishlistPaginated(@Argument PageInput pagination,
                                                  @ContextValue UUID userId) {
        log.info("GQL myWishlistPaginated(user={})", userId);
        Pageable pageable = toPageable(pagination);
        Page<WishlistItemDto> page = wishlistService.getUserWishlistPaginated(userId, pageable);
        return WishlistItemPage.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    @QueryMapping
    public WishlistSummaryDto wishlistSummary(@ContextValue UUID userId) {
        log.info("GQL wishlistSummary(user={})", userId);
        return wishlistService.getWishlistSummary(userId);
    }

    @QueryMapping
    public long wishlistCount(@ContextValue UUID userId) {
        log.info("GQL wishlistCount(user={})", userId);
        return wishlistService.getWishlistCount(userId);
    }

    @QueryMapping
    public boolean isInWishlist(@Argument UUID productId, @ContextValue UUID userId) {
        log.info("GQL isInWishlist(productId={}, user={})", productId, userId);
        return wishlistService.isInWishlist(userId, productId);
    }

    @QueryMapping
    public List<WishlistItemDto> wishlistItemsWithPriceDrops(@ContextValue UUID userId) {
        log.info("GQL wishlistItemsWithPriceDrops(user={})", userId);
        return wishlistService.getItemsWithPriceDrops(userId);
    }

    // =========================================================================
    // MUTATIONS
    // =========================================================================

    @MutationMapping
    public WishlistItemDto addToWishlist(@Argument AddToWishlistInput input,
                                          @ContextValue UUID userId) {
        log.info("GQL addToWishlist(productId={}, user={})", input.getProductId(), userId);
        AddToWishlistRequest request = AddToWishlistRequest.builder()
                .productId(input.getProductId())
                .priority(input.getPriority())
                .notes(input.getNotes())
                .desiredQuantity(input.getDesiredQuantity())
                .targetPrice(input.getTargetPrice())
                .notifyOnPriceDrop(input.getNotifyOnPriceDrop())
                .notifyOnStock(input.getNotifyOnStock())
                .isPublic(input.getIsPublic())
                .collectionName(input.getCollectionName())
                .build();
        return wishlistService.addToWishlist(userId, request);
    }

    @MutationMapping
    public WishlistItemDto updateWishlistItem(@Argument UUID productId,
                                               @Argument UpdateWishlistItemInput input,
                                               @ContextValue UUID userId) {
        log.info("GQL updateWishlistItem(productId={}, user={})", productId, userId);
        UpdateWishlistItemRequest request = UpdateWishlistItemRequest.builder()
                .priority(input.getPriority())
                .notes(input.getNotes())
                .desiredQuantity(input.getDesiredQuantity())
                .targetPrice(input.getTargetPrice())
                .notifyOnPriceDrop(input.getNotifyOnPriceDrop())
                .notifyOnStock(input.getNotifyOnStock())
                .isPublic(input.getIsPublic())
                .build();
        return wishlistService.updateWishlistItem(userId, productId, request);
    }

    @MutationMapping
    public boolean removeFromWishlist(@Argument UUID productId, @ContextValue UUID userId) {
        log.info("GQL removeFromWishlist(productId={}, user={})", productId, userId);
        wishlistService.removeFromWishlist(userId, productId);
        return true;
    }

    @MutationMapping
    public boolean clearWishlist(@ContextValue UUID userId) {
        log.info("GQL clearWishlist(user={})", userId);
        wishlistService.clearWishlist(userId);
        return true;
    }

    @MutationMapping
    public WishlistItemDto markWishlistItemPurchased(@Argument UUID productId,
                                                      @ContextValue UUID userId) {
        log.info("GQL markWishlistItemPurchased(productId={}, user={})", productId, userId);
        return wishlistService.markAsPurchased(userId, productId);
    }

    @MutationMapping
    public boolean moveWishlistItemToCart(@Argument UUID productId, @ContextValue UUID userId) {
        log.info("GQL moveWishlistItemToCart(productId={}, user={})", productId, userId);
        wishlistService.moveToCart(userId, productId);
        return true;
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private Pageable toPageable(PageInput input) {
        if (input == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "addedAt"));
        }
        Sort sort = input.getDirection() == SortDirection.DESC
                ? Sort.by(input.getSortBy()).descending()
                : Sort.by(input.getSortBy()).ascending();
        return PageRequest.of(input.getPage(), input.getSize(), sort);
    }
}

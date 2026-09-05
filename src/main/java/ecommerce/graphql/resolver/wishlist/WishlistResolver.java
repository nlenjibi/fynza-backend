package ecommerce.graphql.resolver.wishlist;

import ecommerce.common.response.PaginatedResponse;
import ecommerce.common.security.UserPrincipal;
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
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public List<WishlistItemDto> myWishlist(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL myWishlist(user={})", principal.getId());
        return wishlistService.getUserWishlist(principal.getId());
    }

    @QueryMapping
    public WishlistItemPage myWishlistPaginated(@Argument PageInput pagination,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL myWishlistPaginated(user={})", principal.getId());
        Pageable pageable = toPageable(pagination);
        Page<WishlistItemDto> page = wishlistService.getUserWishlistPaginated(principal.getId(), pageable);
        return WishlistItemPage.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    @QueryMapping
    public WishlistSummaryDto wishlistSummary(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL wishlistSummary(user={})", principal.getId());
        return wishlistService.getWishlistSummary(principal.getId());
    }

    @QueryMapping
    public long wishlistCount(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL wishlistCount(user={})", principal.getId());
        return wishlistService.getWishlistCount(principal.getId());
    }

    @QueryMapping
    public boolean isInWishlist(@Argument UUID productId,
                                @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL isInWishlist(productId={}, user={})", productId, principal.getId());
        return wishlistService.isInWishlist(principal.getId(), productId);
    }

    @QueryMapping
    public List<WishlistItemDto> wishlistItemsWithPriceDrops(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL wishlistItemsWithPriceDrops(user={})", principal.getId());
        return wishlistService.getItemsWithPriceDrops(principal.getId());
    }

    // =========================================================================
    // MUTATIONS
    // =========================================================================

    @MutationMapping
    public WishlistItemDto addToWishlist(@Argument AddToWishlistInput input,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL addToWishlist(productId={}, user={})", input.getProductId(), principal.getId());
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
        return wishlistService.addToWishlist(principal.getId(), request);
    }

    @MutationMapping
    public WishlistItemDto updateWishlistItem(@Argument UUID productId,
                                              @Argument UpdateWishlistItemInput input,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL updateWishlistItem(productId={}, user={})", productId, principal.getId());
        UpdateWishlistItemRequest request = UpdateWishlistItemRequest.builder()
                .priority(input.getPriority())
                .notes(input.getNotes())
                .desiredQuantity(input.getDesiredQuantity())
                .targetPrice(input.getTargetPrice())
                .notifyOnPriceDrop(input.getNotifyOnPriceDrop())
                .notifyOnStock(input.getNotifyOnStock())
                .isPublic(input.getIsPublic())
                .build();
        return wishlistService.updateWishlistItem(principal.getId(), productId, request);
    }

    @MutationMapping
    public boolean removeFromWishlist(@Argument UUID productId,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL removeFromWishlist(productId={}, user={})", productId, principal.getId());
        wishlistService.removeFromWishlist(principal.getId(), productId);
        return true;
    }

    @MutationMapping
    public boolean clearWishlist(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL clearWishlist(user={})", principal.getId());
        wishlistService.clearWishlist(principal.getId());
        return true;
    }

    @MutationMapping
    public WishlistItemDto markWishlistItemPurchased(@Argument UUID productId,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL markWishlistItemPurchased(productId={}, user={})", productId, principal.getId());
        return wishlistService.markAsPurchased(principal.getId(), productId);
    }

    @MutationMapping
    public boolean moveWishlistItemToCart(@Argument UUID productId,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL moveWishlistItemToCart(productId={}, user={})", productId, principal.getId());
        wishlistService.moveToCart(principal.getId(), productId);
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

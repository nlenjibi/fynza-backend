package ecommerce.graphql.resolver.wishlist;

import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.wishlist.dto.response.WishlistResponse;
import ecommerce.modules.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WishlistResolver {

    private final WishlistService wishlistService;

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<WishlistResponse> myWishlists(@AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL myWishlists user={}", principal.getId());
        return wishlistService.getMyWishlists(principal.getId());
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public WishlistResponse myWishlist(@Argument UUID wishlistId,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL myWishlist wishlistId={} user={}", wishlistId, principal.getId());
        return wishlistService.getWishlist(principal.getId(), wishlistId);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public WishlistResponse myDefaultWishlist(@AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL myDefaultWishlist user={}", principal.getId());
        return wishlistService.getOrCreateDefaultWishlist(principal.getId());
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public boolean isInWishlist(@Argument UUID productId,
                                @Argument UUID variantId,
                                @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL isInWishlist productId={} variantId={} user={}", productId, variantId, principal.getId());
        return wishlistService.isInWishlist(principal.getId(), productId, variantId);
    }

    @QueryMapping
    public WishlistResponse sharedWishlist(@Argument String token) {
        log.debug("GQL sharedWishlist");
        try {
            return wishlistService.getSharedWishlist(token);
        } catch (Exception e) {
            return null;
        }
    }
}

package ecommerce.graphql.resolver.cart;

import ecommerce.modules.cart.dto.CartItemResponse;
import ecommerce.modules.cart.dto.CartResponse;
import ecommerce.modules.cart.repository.CartItemRepository;
import ecommerce.modules.cart.service.CartService;
import ecommerce.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class CartGraphqlController {

    private final CartService cartService;
    private final CartItemRepository cartItemRepository;

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public CartResponse myCart(@AuthenticationPrincipal UserPrincipal principal) {
        return cartService.getCart(principal.getId());
    }

    @QueryMapping
    public CartResponse guestCart(@Argument String cartToken) {
        return cartService.getGuestCart(cartToken);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public CartItemResponse cartItem(@Argument String cartItemId) {
        UUID itemPublicId = UUID.fromString(cartItemId);
        return cartItemRepository.findByPublicId(itemPublicId)
                .map(item -> CartItemResponse.builder()
                        .id(item.getPublicId())
                        .productId(item.getProductId())
                        .variantId(item.getVariantId())
                        .storeId(item.getStoreId())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .lineTotal(item.getLineTotal())
                        .priceChanged(item.getPriceChanged())
                        .priceSnapshotAt(item.getPriceSnapshotAt())
                        .build())
                .orElse(null);
    }
}

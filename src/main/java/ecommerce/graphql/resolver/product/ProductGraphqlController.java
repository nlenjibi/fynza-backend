package ecommerce.graphql.resolver.product;

import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.product.dto.response.ProductResponse;
import ecommerce.modules.product.dto.response.ProductVariantResponse;
import ecommerce.modules.product.service.ProductService;
import ecommerce.modules.product.service.ProductVariantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
public class ProductGraphqlController {

    private final ProductService        productService;
    private final ProductVariantService variantService;

    @QueryMapping
    public ProductResponse product(@Argument String id) {
        log.debug("GQL product id={}", id);
        return productService.findById(UUID.fromString(id));
    }

    @QueryMapping
    public ProductResponse productBySlug(@Argument String slug) {
        log.debug("GQL productBySlug slug={}", slug);
        return productService.findBySlug(slug);
    }

    @QueryMapping
    public Page<ProductResponse> products(@Argument Integer page, @Argument Integer size) {
        log.debug("GQL products page={} size={}", page, size);
        return productService.findPublicProducts(
                PageRequest.of(page != null ? page : 0, size != null ? size : 20,
                        Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @QueryMapping
    public Page<ProductResponse> productsByStore(@Argument String storeId,
                                                 @Argument Integer page,
                                                 @Argument Integer size) {
        log.debug("GQL productsByStore storeId={}", storeId);
        return productService.findByStore(UUID.fromString(storeId),
                PageRequest.of(page != null ? page : 0, size != null ? size : 20,
                        Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @QueryMapping
    public List<ProductVariantResponse> productVariants(@Argument String productId) {
        log.debug("GQL productVariants productId={}", productId);
        return variantService.getVariants(UUID.fromString(productId));
    }

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public Page<ProductResponse> myProducts(@Argument String storeId,
                                            @Argument Integer page,
                                            @Argument Integer size,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL myProducts storeId={} userId={}", storeId, principal.getId());
        return productService.findByStore(UUID.fromString(storeId),
                PageRequest.of(page != null ? page : 0, size != null ? size : 20,
                        Sort.by(Sort.Direction.DESC, "createdAt")));
    }
}

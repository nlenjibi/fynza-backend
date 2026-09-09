package ecommerce.graphql.resolver.seller;

import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.seller.dto.request.SellerSearchRequest;
import ecommerce.modules.seller.dto.response.SellerDetailResponse;
import ecommerce.modules.seller.dto.response.SellerStatusHistoryResponse;
import ecommerce.modules.seller.dto.response.SellerSummaryResponse;
import ecommerce.modules.seller.enums.SellerType;
import ecommerce.modules.seller.service.SellerService;
import ecommerce.common.enums.SellerStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SellerResolver {

    private final SellerService sellerService;

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public SellerDetailResponse mySeller(@AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL mySeller userId={}", principal.getId());
        return sellerService.getMySeller(principal.getId());
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public SellerDetailResponse seller(@Argument String id) {
        return sellerService.getSellerByPublicId(UUID.fromString(id));
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<SellerSummaryResponse> sellers(@Argument Map<String, Object> filter) {
        SellerSearchRequest req = new SellerSearchRequest();
        if (filter != null) {
            if (filter.get("query") != null) req.setQuery((String) filter.get("query"));
            if (filter.get("status") != null) req.setStatus(SellerStatus.valueOf((String) filter.get("status")));
            if (filter.get("sellerType") != null) req.setSellerType(SellerType.valueOf((String) filter.get("sellerType")));
        }
        int page = filter != null && filter.get("page") != null ? (int) filter.get("page") : 0;
        int size = filter != null && filter.get("size") != null ? (int) filter.get("size") : 20;
        return sellerService.searchSellers(req, PageRequest.of(page, size));
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<SellerStatusHistoryResponse> sellerStatusHistory(@Argument String sellerId) {
        return sellerService.getStatusHistory(UUID.fromString(sellerId));
    }
}

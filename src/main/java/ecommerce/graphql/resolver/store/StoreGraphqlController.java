package ecommerce.graphql.resolver.store;

import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.store.dto.request.StoreSearchRequest;
import ecommerce.modules.store.dto.response.*;
import ecommerce.modules.store.enums.StoreStatus;
import ecommerce.modules.store.enums.StoreVisibility;
import ecommerce.modules.store.service.StoreManagementService;
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
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class StoreGraphqlController {

    private final StoreManagementService storeManagementService;

    @QueryMapping
    @PreAuthorize("hasAuthority('store.read.own')")
    public StoreDetailResponse myStore(@AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL myStore userId={}", principal.getId());
        return storeManagementService.getMyStore(principal.getId());
    }

    @QueryMapping
    public StoreSummaryResponse storeBySlug(@Argument String slug) {
        log.debug("GQL storeBySlug slug={}", slug);
        return storeManagementService.getStoreBySlug(slug);
    }

    @QueryMapping
    public Page<StoreSummaryResponse> stores(
            @Argument String query,
            @Argument String status,
            @Argument String visibility,
            @Argument Integer page,
            @Argument Integer size) {

        StoreSearchRequest params = new StoreSearchRequest();
        params.setQuery(query);
        if (status != null)     params.setStatus(StoreStatus.valueOf(status));
        if (visibility != null) params.setVisibility(StoreVisibility.valueOf(visibility));

        return storeManagementService.searchStores(params,
                PageRequest.of(page != null ? page : 0, size != null ? size : 20));
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('store.read')")
    public List<StoreStatusHistoryResponse> storeStatusHistory(@Argument String storeId) {
        log.debug("GQL storeStatusHistory storeId={}", storeId);
        return storeManagementService.getStatusHistory(UUID.fromString(storeId));
    }
}

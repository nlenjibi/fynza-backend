package ecommerce.graphql.resolver.store;

import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.store.dto.SellerNotificationSettingsResponse;
import ecommerce.modules.store.dto.SellerPaymentSettingsResponse;
import ecommerce.modules.store.dto.SellerShippingSettingsResponse;
import ecommerce.modules.store.dto.ShippingZoneResponse;
import ecommerce.modules.store.dto.StoreResponse;
import ecommerce.modules.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class StoreResolver {

    private final StoreService storeService;

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public StoreResponse sellerStore(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL sellerStore(seller={})", principal.getId());
        return storeService.getStore(principal.getId());
    }

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public SellerPaymentSettingsResponse sellerPaymentSettings(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL sellerPaymentSettings(seller={})", principal.getId());
        return storeService.getPaymentSettings(principal.getId());
    }

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public SellerShippingSettingsResponse sellerShippingSettings(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL sellerShippingSettings(seller={})", principal.getId());
        return storeService.getShippingSettings(principal.getId());
    }

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public List<ShippingZoneResponse> sellerShippingZones(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL sellerShippingZones(seller={})", principal.getId());
        return storeService.getShippingZones(principal.getId());
    }

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public SellerNotificationSettingsResponse sellerNotificationSettings(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL sellerNotificationSettings(seller={})", principal.getId());
        return storeService.getNotificationSettings(principal.getId());
    }
}

package ecommerce.graphql.resolver.shipping;

import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.shipping.dto.response.*;
import ecommerce.modules.shipping.enums.FulfillmentStatus;
import ecommerce.modules.shipping.service.CarrierAdminService;
import ecommerce.modules.shipping.service.FulfillmentService;
import ecommerce.modules.shipping.service.ShipmentService;
import lombok.RequiredArgsConstructor;
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
public class ShippingQueryResolver {

    private final ShipmentService shipmentService;
    private final FulfillmentService fulfillmentService;
    private final CarrierAdminService carrierAdminService;

    @QueryMapping
    public ShipmentResponse shipmentTracking(@Argument String trackingNumber) {
        return shipmentService.getByTrackingNumber(trackingNumber);
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('shipping:write')")
    public Page<FulfillmentResponse> myFulfillments(
            @AuthenticationPrincipal UserPrincipal principal,
            @Argument String status,
            @Argument int page,
            @Argument int size) {
        FulfillmentStatus statusEnum = status != null ? FulfillmentStatus.valueOf(status) : null;
        return fulfillmentService.getSellerFulfillments(
                principal.getId(), statusEnum,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('shipping:write')")
    public FulfillmentResponse myFulfillment(
            @AuthenticationPrincipal UserPrincipal principal,
            @Argument String id) {
        return fulfillmentService.getFulfillmentForSeller(principal.getId(), UUID.fromString(id));
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('shipping:write')")
    public List<ShipmentResponse> myShipments(@Argument String fulfillmentId) {
        return shipmentService.getShipmentsByFulfillment(UUID.fromString(fulfillmentId));
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<ShipmentResponse> orderShipments(@Argument String orderId) {
        return shipmentService.getShipmentsForOrder(UUID.fromString(orderId));
    }

    @QueryMapping
    public List<ShippingMethodResponse> shippingMethods() {
        return carrierAdminService.getAllActiveMethods();
    }

    @QueryMapping
    public List<CarrierResponse> carriers() {
        return carrierAdminService.getAllCarriers();
    }
}

package ecommerce.graphql.resolver.shipping;

import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.shipping.dto.request.CreateShipmentRequest;
import ecommerce.modules.shipping.dto.request.RecordTrackingEventRequest;
import ecommerce.modules.shipping.dto.request.ResolveExceptionRequest;
import ecommerce.modules.shipping.dto.request.ShipmentExceptionRequest;
import ecommerce.modules.shipping.dto.request.UpdateShipmentStatusRequest;
import ecommerce.modules.shipping.dto.response.ShipmentExceptionResponse;
import ecommerce.modules.shipping.dto.response.ShipmentResponse;
import ecommerce.modules.shipping.dto.response.TrackingEventResponse;
import ecommerce.modules.shipping.enums.ExceptionSeverity;
import ecommerce.modules.shipping.enums.ExceptionType;
import ecommerce.modules.shipping.enums.ShipmentStatus;
import ecommerce.modules.shipping.service.ShipmentExceptionService;
import ecommerce.modules.shipping.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;


import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ShippingMutationResolver {

    private final ShipmentService shipmentService;
    private final ShipmentExceptionService exceptionService;

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public ShipmentResponse createShipment(
            @AuthenticationPrincipal UserPrincipal principal,
            @Argument CreateShipmentRequest input) {
        return shipmentService.createShipment(principal.getId(), input);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public ShipmentResponse updateShipmentStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @Argument String id,
            @Argument String status) {
        UpdateShipmentStatusRequest request = new UpdateShipmentStatusRequest();
        request.setStatus(ShipmentStatus.valueOf(status));
        return shipmentService.updateStatus(principal.getId(), UUID.fromString(id), request);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public ShipmentResponse cancelShipment(
            @AuthenticationPrincipal UserPrincipal principal,
            @Argument String id) {
        shipmentService.cancelShipment(principal.getId(), UUID.fromString(id));
        return shipmentService.getShipment(UUID.fromString(id));
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public ShipmentResponse generateShipmentLabel(
            @AuthenticationPrincipal UserPrincipal principal,
            @Argument String id) {
        return shipmentService.generateLabel(principal.getId(), UUID.fromString(id));
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public TrackingEventResponse recordTrackingEvent(@Argument String shipmentId,
                                                     @Argument String status,
                                                     @Argument String description,
                                                     @Argument String location) {
        RecordTrackingEventRequest request = new RecordTrackingEventRequest();
        request.setStatus(ShipmentStatus.valueOf(status));
        request.setDescription(description);
        request.setLocation(location);
        return shipmentService.recordTrackingEvent(UUID.fromString(shipmentId), request);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public ShipmentExceptionResponse createShipmentException(
            @Argument String shipmentId,
            @Argument String type,
            @Argument String severity,
            @Argument String description) {
        ShipmentExceptionRequest request = new ShipmentExceptionRequest();
        request.setType(ExceptionType.valueOf(type));
        request.setSeverity(ExceptionSeverity.valueOf(severity));
        request.setDescription(description);
        return exceptionService.createException(UUID.fromString(shipmentId), request);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ShipmentExceptionResponse resolveShipmentException(
            @Argument String id,
            @Argument String resolutionNotes,
            @Argument String resolvedBy) {
        ResolveExceptionRequest request = new ResolveExceptionRequest();
        request.setResolutionNotes(resolutionNotes);
        request.setResolvedBy(resolvedBy);
        return exceptionService.resolveException(UUID.fromString(id), request);
    }
}

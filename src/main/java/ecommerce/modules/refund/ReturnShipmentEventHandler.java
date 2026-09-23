package ecommerce.modules.refund;

import ecommerce.modules.refund.repository.ReturnRepository;
import ecommerce.modules.refund.service.ReturnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Handles shipping lifecycle events that affect returns.
 * Called by the Shipping module's webhook processor when a return shipment
 * transitions (e.g. RETURN_SHIPMENT_DELIVERED → mark the return as RECEIVED).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReturnShipmentEventHandler {

    private final ReturnRepository returnRepository;
    private final ReturnService returnService;

    /** Called when the carrier confirms delivery of the return package to the warehouse. */
    public void onReturnShipmentDelivered(UUID shipmentPublicId, UUID triggeredBy) {
        returnRepository.findByReturnShipmentId(shipmentPublicId).ifPresentOrElse(
                returnEntity -> {
                    log.info("Shipping event: return shipment {} delivered — marking return {} as RECEIVED",
                            shipmentPublicId, returnEntity.getReturnNumber());
                    returnService.markReceived(returnEntity.getPublicId(), triggeredBy);
                },
                () -> log.warn("Shipping event: no return found for shipmentId={}", shipmentPublicId)
        );
    }

    /** Called when the carrier picks up the return package (optional, for status granularity). */
    public void onReturnShipmentPickedUp(UUID shipmentPublicId, UUID triggeredBy) {
        returnRepository.findByReturnShipmentId(shipmentPublicId).ifPresentOrElse(
                returnEntity -> {
                    log.info("Shipping event: return shipment {} picked up — marking return {} as IN_TRANSIT",
                            shipmentPublicId, returnEntity.getReturnNumber());
                    returnService.markInTransit(returnEntity.getPublicId(), triggeredBy);
                },
                () -> log.warn("Shipping event: no return found for shipmentId={}", shipmentPublicId)
        );
    }
}

package ecommerce.modules.shipping.service.impl;

import ecommerce.modules.shipping.ShipmentStateMachine;
import ecommerce.modules.shipping.dto.request.DeliveryAttemptRequest;
import ecommerce.modules.shipping.dto.response.DeliveryAttemptResponse;
import ecommerce.modules.shipping.entity.DeliveryAttempt;
import ecommerce.modules.shipping.entity.Shipment;
import ecommerce.modules.shipping.enums.ShipmentStatus;
import ecommerce.modules.shipping.exception.ShipmentNotFoundException;
import ecommerce.modules.shipping.repository.DeliveryAttemptRepository;
import ecommerce.modules.shipping.repository.ShipmentRepository;
import ecommerce.modules.shipping.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final ShipmentRepository shipmentRepository;

    @Override
    @Transactional
    public DeliveryAttemptResponse recordAttempt(UUID shipmentPublicId, DeliveryAttemptRequest request) {
        Shipment shipment = shipmentRepository.findByPublicId(shipmentPublicId)
                .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found: " + shipmentPublicId));

        int attemptNumber = deliveryAttemptRepository.countByShipmentId(shipment.getPublicId()) + 1;

        DeliveryAttempt attempt = DeliveryAttempt.builder()
                .shipmentId(shipment.getPublicId())
                .attemptNumber(attemptNumber)
                .status(request.getStatus())
                .failureReason(request.getFailureReason())
                .location(request.getLocation())
                .notes(request.getNotes())
                .nextAttemptAt(request.getNextAttemptAt())
                .build();

        attempt = deliveryAttemptRepository.save(attempt);

        if (attemptNumber >= 3 && shipment.getStatus() == ShipmentStatus.DELIVERY_FAILED
                && ShipmentStateMachine.canTransition(shipment.getStatus(), ShipmentStatus.RETURN_TO_SENDER)) {
            shipment.setStatus(ShipmentStatus.RETURN_TO_SENDER);
            shipmentRepository.save(shipment);
            log.info("Auto-transitioned shipment={} to RETURN_TO_SENDER after {} delivery attempts",
                    shipment.getShipmentNumber(), attemptNumber);
        }

        log.info("Recorded delivery attempt #{} for shipment={}", attemptNumber, shipment.getShipmentNumber());
        return DeliveryAttemptResponse.from(attempt);
    }

    @Override
    public List<DeliveryAttemptResponse> getAttempts(UUID shipmentPublicId) {
        Shipment shipment = shipmentRepository.findByPublicId(shipmentPublicId)
                .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found: " + shipmentPublicId));
        return deliveryAttemptRepository.findByShipmentIdOrderByAttemptNumberAsc(shipment.getPublicId())
                .stream().map(DeliveryAttemptResponse::from).toList();
    }
}

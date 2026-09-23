package ecommerce.modules.shipping.service.impl;

import ecommerce.common.exception.BadRequestException;
import ecommerce.modules.shipping.dto.request.CreateShipmentRequest;
import ecommerce.modules.shipping.dto.request.ShipmentItemRequest;
import ecommerce.modules.shipping.service.ShippingValidationService;
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
public class ShippingValidationServiceImpl implements ShippingValidationService {

    @Override
    public void validateShipmentCreation(CreateShipmentRequest request, UUID sellerPublicId) {
        if (request.getFulfillmentId() == null) {
            throw new BadRequestException("Fulfillment ID is required");
        }

        List<ShipmentItemRequest> items = request.getItems();
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("At least one shipment item is required");
        }

        for (ShipmentItemRequest item : items) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BadRequestException("Each shipment item must have a quantity greater than 0");
            }
        }

        log.debug("Shipment creation validated for seller={} fulfillment={}",
                sellerPublicId, request.getFulfillmentId());
    }
}

package ecommerce.modules.shipping.service;

import ecommerce.modules.shipping.dto.request.CreateShipmentRequest;

import java.util.UUID;

public interface ShippingValidationService {

    void validateShipmentCreation(CreateShipmentRequest request, UUID sellerPublicId);
}

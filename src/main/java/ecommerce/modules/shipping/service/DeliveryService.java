package ecommerce.modules.shipping.service;

import ecommerce.modules.shipping.dto.request.DeliveryAttemptRequest;
import ecommerce.modules.shipping.dto.response.DeliveryAttemptResponse;

import java.util.List;
import java.util.UUID;

public interface DeliveryService {

    DeliveryAttemptResponse recordAttempt(UUID shipmentPublicId, DeliveryAttemptRequest request);

    List<DeliveryAttemptResponse> getAttempts(UUID shipmentPublicId);
}

package ecommerce.modules.shipping.service;

import ecommerce.modules.shipping.dto.request.CreateFulfillmentRequest;
import ecommerce.modules.shipping.dto.response.FulfillmentResponse;
import ecommerce.modules.shipping.enums.FulfillmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface FulfillmentService {

    FulfillmentResponse createFulfillment(UUID userId, CreateFulfillmentRequest request);

    FulfillmentResponse getFulfillment(UUID fulfillmentPublicId);

    FulfillmentResponse getFulfillmentForSeller(UUID userId, UUID fulfillmentPublicId);

    Page<FulfillmentResponse> getSellerFulfillments(UUID userId, FulfillmentStatus status, Pageable pageable);

    FulfillmentResponse updateStatus(UUID userId, UUID fulfillmentPublicId, FulfillmentStatus newStatus, String notes);
}

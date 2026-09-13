package ecommerce.modules.customer.service;

import ecommerce.modules.customer.dto.request.CustomerPreferenceRequest;
import ecommerce.modules.customer.dto.response.CustomerPreferenceResponse;

import java.util.UUID;

public interface CustomerPreferenceService {

    CustomerPreferenceResponse getMyPreferences(UUID userId);

    CustomerPreferenceResponse updatePreferences(UUID userId, CustomerPreferenceRequest request);
}

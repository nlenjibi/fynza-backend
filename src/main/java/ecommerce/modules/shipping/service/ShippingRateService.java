package ecommerce.modules.shipping.service;

import ecommerce.modules.shipping.dto.request.CalculateShippingRateRequest;
import ecommerce.modules.shipping.dto.response.ShippingRateResponse;

import java.util.List;

public interface ShippingRateService {

    List<ShippingRateResponse> calculateRates(CalculateShippingRateRequest request);

    List<ShippingRateResponse> getAllActiveRates();
}

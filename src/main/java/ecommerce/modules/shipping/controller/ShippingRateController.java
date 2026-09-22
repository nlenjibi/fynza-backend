package ecommerce.modules.shipping.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.shipping.dto.request.CalculateShippingRateRequest;
import ecommerce.modules.shipping.dto.response.ShippingRateResponse;
import ecommerce.modules.shipping.service.ShippingRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/shipping")
@RequiredArgsConstructor
@Tag(name = "Shipping Rates", description = "Calculate available shipping rates")
public class ShippingRateController {

    private final ShippingRateService shippingRateService;

    @PostMapping("/rates")
    @Operation(summary = "Calculate available shipping rates for a destination and weight")
    public ResponseEntity<ApiResponse<List<ShippingRateResponse>>> calculateRates(
            @Valid @RequestBody CalculateShippingRateRequest request) {
        List<ShippingRateResponse> rates = shippingRateService.calculateRates(request);
        return ResponseEntity.ok(ApiResponse.success("Shipping rates calculated", rates));
    }
}

package ecommerce.modules.delivery.service;

import ecommerce.modules.delivery.dto.DeliveryFeeRequest;
import ecommerce.modules.delivery.dto.DeliveryFeeResponse;
import ecommerce.modules.delivery.dto.DeliveryRegionRequest;
import ecommerce.modules.delivery.dto.DeliveryRegionResponse;
import ecommerce.modules.delivery.entity.DeliveryFee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface DeliveryService {

    // Region operations
    DeliveryRegionResponse createRegion(DeliveryRegionRequest request);

    DeliveryRegionResponse updateRegion(UUID id, DeliveryRegionRequest request);

    void deleteRegion(UUID id);

    Page<DeliveryRegionResponse> getAllRegions(Pageable pageable);

    DeliveryRegionResponse getRegionById(UUID id);

    List<DeliveryRegionResponse> getActiveRegions();

    // Fee operations
    DeliveryFeeResponse createDeliveryFee(DeliveryFeeRequest request);

    DeliveryFeeResponse updateDeliveryFee(UUID id, DeliveryFeeRequest request);

    void deleteDeliveryFee(UUID id);

    Page<DeliveryFeeResponse> getAllDeliveryFees(Pageable pageable);

    DeliveryFeeResponse getDeliveryFeeById(UUID id);

    List<DeliveryFeeResponse> getActiveDeliveryFees();

    List<DeliveryFeeResponse> getDeliveryFeesByRegion(UUID regionId);
}

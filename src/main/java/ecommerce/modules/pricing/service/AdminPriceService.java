package ecommerce.modules.pricing.service;

import ecommerce.modules.pricing.dto.request.PriceOverrideRequest;
import ecommerce.modules.pricing.dto.request.UpdatePriceRequest;
import ecommerce.modules.pricing.dto.response.PriceHistoryResponse;
import ecommerce.modules.pricing.dto.response.PriceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface AdminPriceService {

    Page<PriceResponse> getAllPrices(Pageable pageable);

    PriceResponse getPriceById(UUID publicId);

    PriceResponse updatePrice(UUID publicId, UpdatePriceRequest request, UUID adminId);

    PriceResponse overridePrice(UUID publicId, PriceOverrideRequest request, UUID adminId);

    List<PriceHistoryResponse> getPriceHistory(UUID publicId);

    List<PriceResponse> getPricesByProductId(UUID productId);
}

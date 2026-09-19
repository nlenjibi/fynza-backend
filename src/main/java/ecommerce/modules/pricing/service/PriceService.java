package ecommerce.modules.pricing.service;

import ecommerce.modules.pricing.dto.request.CreatePriceRequest;
import ecommerce.modules.pricing.dto.request.CreatePriceTierRequest;
import ecommerce.modules.pricing.dto.request.SchedulePriceRequest;
import ecommerce.modules.pricing.dto.request.UpdatePriceRequest;
import ecommerce.modules.pricing.dto.response.PriceHistoryResponse;
import ecommerce.modules.pricing.dto.response.PriceResponse;
import ecommerce.modules.pricing.dto.response.PriceTierResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PriceService {

    PriceResponse createPrice(CreatePriceRequest request, UUID actorId);

    PriceResponse updatePrice(UUID publicId, UpdatePriceRequest request, UUID actorId);

    PriceResponse activatePrice(UUID publicId, UUID actorId);

    PriceResponse disablePrice(UUID publicId, UUID actorId);

    PriceResponse schedulePrice(UUID publicId, SchedulePriceRequest request, UUID actorId);

    Page<PriceResponse> getSellerPrices(UUID productId, UUID actorId, Pageable pageable);

    List<PriceHistoryResponse> getPriceHistory(UUID publicId, UUID actorId);

    PriceTierResponse createTier(CreatePriceTierRequest request, UUID actorId);

    PriceTierResponse updateTier(UUID tierPublicId, CreatePriceTierRequest request, UUID actorId);

    void deleteTier(UUID tierPublicId, UUID actorId);
}

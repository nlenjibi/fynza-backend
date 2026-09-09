package ecommerce.modules.seller.service;

import ecommerce.modules.seller.dto.request.SellerOnboardingRequest;
import ecommerce.modules.seller.dto.response.SellerDetailResponse;
import ecommerce.modules.seller.dto.response.SellerResponse;

import java.util.UUID;

public interface SellerOnboardingService {

    SellerDetailResponse saveOnboarding(UUID userId, SellerOnboardingRequest request);

    SellerResponse submitApplication(UUID userId);
}

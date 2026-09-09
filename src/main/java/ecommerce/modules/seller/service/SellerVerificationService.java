package ecommerce.modules.seller.service;

import ecommerce.modules.seller.dto.response.SellerVerificationResponse;
import ecommerce.modules.seller.enums.VerificationType;

import java.util.List;
import java.util.UUID;

public interface SellerVerificationService {

    List<SellerVerificationResponse> getMyVerifications(UUID userId);

    SellerVerificationResponse submitVerification(UUID userId, VerificationType type);

    SellerVerificationResponse adminReviewVerification(UUID sellerPublicId, VerificationType type,
                                                        boolean approved, String reason, UUID reviewerId);
}

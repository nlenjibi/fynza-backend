package ecommerce.modules.seller.mapper;

import ecommerce.modules.seller.dto.response.*;
import ecommerce.modules.seller.entity.Seller;
import ecommerce.modules.seller.entity.SellerBusiness;
import ecommerce.modules.seller.entity.SellerStatusHistory;
import ecommerce.modules.seller.entity.SellerVerification;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SellerMapper {

    public SellerResponse toResponse(Seller seller) {
        return SellerResponse.builder()
                .publicId(seller.getPublicId())
                .sellerNumber(seller.getSellerNumber())
                .displayName(seller.getDisplayName())
                .sellerType(seller.getSellerType())
                .status(seller.getStatus())
                .createdAt(seller.getCreatedAt())
                .updatedAt(seller.getUpdatedAt())
                .build();
    }

    public SellerSummaryResponse toSummary(Seller seller) {
        return SellerSummaryResponse.builder()
                .publicId(seller.getPublicId())
                .sellerNumber(seller.getSellerNumber())
                .displayName(seller.getDisplayName())
                .sellerType(seller.getSellerType())
                .status(seller.getStatus())
                .createdAt(seller.getCreatedAt())
                .build();
    }

    public SellerDetailResponse toDetailResponse(Seller seller, SellerBusiness business,
                                                  List<SellerVerification> verifications) {
        return SellerDetailResponse.builder()
                .publicId(seller.getPublicId())
                .sellerNumber(seller.getSellerNumber())
                .displayName(seller.getDisplayName())
                .sellerType(seller.getSellerType())
                .status(seller.getStatus())
                .business(business != null ? toBusinessResponse(business) : null)
                .verifications(verifications.stream().map(this::toVerificationResponse).toList())
                .createdAt(seller.getCreatedAt())
                .updatedAt(seller.getUpdatedAt())
                .build();
    }

    public SellerBusinessResponse toBusinessResponse(SellerBusiness b) {
        return SellerBusinessResponse.builder()
                .publicId(b.getPublicId())
                .legalName(b.getLegalName())
                .businessName(b.getBusinessName())
                .businessType(b.getBusinessType())
                .registrationNumber(b.getRegistrationNumber())
                .taxIdentifier(b.getTaxIdentifier())
                .description(b.getDescription())
                .website(b.getWebsite())
                .email(b.getEmail())
                .phone(b.getPhone())
                .country(b.getCountry())
                .region(b.getRegion())
                .city(b.getCity())
                .address(b.getAddress())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }

    public SellerStatusHistoryResponse toStatusHistoryResponse(SellerStatusHistory h) {
        return SellerStatusHistoryResponse.builder()
                .previousStatus(h.getPreviousStatus())
                .newStatus(h.getNewStatus())
                .reason(h.getReason())
                .changedBy(h.getChangedBy())
                .expiresAt(h.getExpiresAt())
                .createdAt(h.getCreatedAt())
                .build();
    }

    public SellerVerificationResponse toVerificationResponse(SellerVerification v) {
        return SellerVerificationResponse.builder()
                .publicId(v.getPublicId())
                .verificationType(v.getVerificationType())
                .status(v.getStatus())
                .submittedAt(v.getSubmittedAt())
                .reviewedAt(v.getReviewedAt())
                .rejectionReason(v.getRejectionReason())
                .expiresAt(v.getExpiresAt())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }
}

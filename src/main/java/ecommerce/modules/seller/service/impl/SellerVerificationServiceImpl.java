package ecommerce.modules.seller.service.impl;

import ecommerce.modules.seller.dto.response.SellerVerificationResponse;
import ecommerce.modules.seller.entity.SellerVerification;
import ecommerce.modules.seller.enums.VerificationItemStatus;
import ecommerce.modules.seller.enums.VerificationType;
import ecommerce.modules.seller.exception.SellerNotFoundException;
import ecommerce.modules.seller.mapper.SellerMapper;
import ecommerce.modules.seller.policy.SellerOwnershipPolicy;
import ecommerce.modules.seller.repository.SellerRepository;
import ecommerce.modules.seller.repository.SellerVerificationRepository;
import ecommerce.modules.seller.service.SellerVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerVerificationServiceImpl implements SellerVerificationService {

    private final SellerVerificationRepository verificationRepository;
    private final SellerRepository             sellerRepository;
    private final SellerOwnershipPolicy        ownershipPolicy;
    private final SellerMapper                 mapper;

    @Override
    public List<SellerVerificationResponse> getMyVerifications(UUID userId) {
        var seller = ownershipPolicy.resolveOwn(userId);
        return verificationRepository.findBySellerId(seller.getId())
                .stream()
                .map(mapper::toVerificationResponse)
                .toList();
    }

    @Override
    @Transactional
    public SellerVerificationResponse submitVerification(UUID userId, VerificationType type) {
        var seller = ownershipPolicy.resolveOwn(userId);

        SellerVerification verification = verificationRepository
                .findBySellerIdAndVerificationType(seller.getId(), type)
                .orElseGet(() -> SellerVerification.builder()
                        .sellerId(seller.getId())
                        .verificationType(type)
                        .build());

        verification.setStatus(VerificationItemStatus.PENDING);
        verification.setSubmittedAt(Instant.now());
        verification.setRejectionReason(null);
        verificationRepository.save(verification);

        log.info("Verification submitted: seller={}, type={}", seller.getSellerNumber(), type);
        return mapper.toVerificationResponse(verification);
    }

    @Override
    @Transactional
    public SellerVerificationResponse adminReviewVerification(UUID sellerPublicId, VerificationType type,
                                                               boolean approved, String reason, UUID reviewerId) {
        var seller = sellerRepository.findByPublicId(sellerPublicId)
                .orElseThrow(() -> new SellerNotFoundException(sellerPublicId));

        SellerVerification verification = verificationRepository
                .findBySellerIdAndVerificationType(seller.getId(), type)
                .orElseThrow(() -> new RuntimeException("Verification record not found for type: " + type));

        verification.setStatus(approved ? VerificationItemStatus.VERIFIED : VerificationItemStatus.REJECTED);
        verification.setReviewedAt(Instant.now());
        verification.setReviewedBy(reviewerId);
        if (!approved) verification.setRejectionReason(reason);
        verificationRepository.save(verification);

        log.info("Verification reviewed: seller={}, type={}, approved={}", seller.getSellerNumber(), type, approved);
        return mapper.toVerificationResponse(verification);
    }
}

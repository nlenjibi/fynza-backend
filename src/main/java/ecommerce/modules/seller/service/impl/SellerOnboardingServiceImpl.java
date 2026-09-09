package ecommerce.modules.seller.service.impl;

import ecommerce.common.enums.SellerStatus;
import ecommerce.modules.audit.constant.AuditAction;
import ecommerce.modules.audit.dto.AuditLogEntry;
import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.seller.dto.request.SellerOnboardingRequest;
import ecommerce.modules.seller.dto.response.SellerDetailResponse;
import ecommerce.modules.seller.dto.response.SellerResponse;
import ecommerce.modules.seller.entity.Seller;
import ecommerce.modules.seller.entity.SellerBusiness;
import ecommerce.modules.seller.entity.SellerStatusHistory;
import ecommerce.modules.seller.mapper.SellerMapper;
import ecommerce.modules.seller.policy.SellerOwnershipPolicy;
import ecommerce.modules.seller.repository.SellerBusinessRepository;
import ecommerce.modules.seller.repository.SellerRepository;
import ecommerce.modules.seller.repository.SellerStatusHistoryRepository;
import ecommerce.modules.seller.repository.SellerVerificationRepository;
import ecommerce.modules.seller.service.SellerOnboardingService;
import ecommerce.modules.seller.validation.SellerStatusTransitionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerOnboardingServiceImpl implements SellerOnboardingService {

    private final SellerRepository                sellerRepository;
    private final SellerBusinessRepository        businessRepository;
    private final SellerStatusHistoryRepository   historyRepository;
    private final SellerVerificationRepository    verificationRepository;
    private final SellerOwnershipPolicy           ownershipPolicy;
    private final SellerStatusTransitionValidator transitionValidator;
    private final SellerMapper                    mapper;
    private final AuditLogService                 auditLogService;

    @Override
    @Transactional
    public SellerDetailResponse saveOnboarding(UUID userId, SellerOnboardingRequest request) {
        Seller seller = ownershipPolicy.resolveOwn(userId);

        SellerBusiness business = businessRepository.findBySellerId(seller.getId())
                .orElseGet(() -> SellerBusiness.builder().sellerId(seller.getId()).build());

        if (request.getLegalName()          != null) business.setLegalName(request.getLegalName());
        if (request.getBusinessName()       != null) business.setBusinessName(request.getBusinessName());
        if (request.getBusinessType()       != null) business.setBusinessType(request.getBusinessType());
        if (request.getRegistrationNumber() != null) business.setRegistrationNumber(request.getRegistrationNumber());
        if (request.getTaxIdentifier()      != null) business.setTaxIdentifier(request.getTaxIdentifier());
        if (request.getDescription()        != null) business.setDescription(request.getDescription());
        if (request.getWebsite()            != null) business.setWebsite(request.getWebsite());
        if (request.getEmail()              != null) business.setEmail(request.getEmail());
        if (request.getPhone()              != null) business.setPhone(request.getPhone());
        if (request.getCountry()            != null) business.setCountry(request.getCountry());
        if (request.getRegion()             != null) business.setRegion(request.getRegion());
        if (request.getCity()               != null) business.setCity(request.getCity());
        if (request.getAddress()            != null) business.setAddress(request.getAddress());

        businessRepository.save(business);
        return mapper.toDetailResponse(seller, business, verificationRepository.findBySellerId(seller.getId()));
    }

    @Override
    @Transactional
    public SellerResponse submitApplication(UUID userId) {
        Seller seller = ownershipPolicy.resolveOwn(userId);

        SellerStatus previous = seller.getStatus();
        transitionValidator.validate(previous, SellerStatus.PENDING_VERIFICATION);

        seller.setStatus(SellerStatus.PENDING_VERIFICATION);
        sellerRepository.save(seller);

        historyRepository.save(SellerStatusHistory.builder()
                .sellerId(seller.getId())
                .previousStatus(previous)
                .newStatus(SellerStatus.PENDING_VERIFICATION)
                .reason("Seller submitted application for verification")
                .changedBy(userId)
                .build());

        auditLogService.log(AuditLogEntry.builder()
                .action(AuditAction.SELLER_REGISTERED)
                .actorPublicId(userId)
                .entityType("SELLER")
                .entityPublicId(seller.getPublicId())
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());

        log.info("Seller application submitted: sellerNumber={}", seller.getSellerNumber());
        return mapper.toResponse(seller);
    }
}

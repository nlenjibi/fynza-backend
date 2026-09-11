package ecommerce.modules.seller.service.impl;

import ecommerce.modules.audit.constant.AuditAction;
import ecommerce.modules.audit.dto.AuditLogEntry;
import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.seller.dto.request.SellerSearchRequest;
import ecommerce.modules.seller.dto.request.UpdateSellerRequest;
import ecommerce.modules.seller.dto.response.*;
import ecommerce.modules.seller.entity.Seller;
import ecommerce.modules.seller.entity.SellerBusiness;
import ecommerce.modules.seller.entity.SellerVerification;
import ecommerce.modules.seller.exception.SellerAlreadyExistsException;
import ecommerce.modules.seller.exception.SellerNotFoundException;
import ecommerce.modules.seller.mapper.SellerMapper;
import ecommerce.modules.seller.policy.SellerOwnershipPolicy;
import ecommerce.modules.seller.repository.SellerBusinessRepository;
import ecommerce.modules.seller.repository.SellerRepository;
import ecommerce.modules.seller.repository.SellerStatusHistoryRepository;
import ecommerce.modules.seller.repository.SellerVerificationRepository;
import ecommerce.modules.seller.service.SellerNumberService;
import ecommerce.modules.seller.service.SellerService;
import ecommerce.modules.seller.spec.SellerSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerServiceImpl implements SellerService {

    private final SellerRepository             sellerRepository;
    private final SellerBusinessRepository     businessRepository;
    private final SellerStatusHistoryRepository historyRepository;
    private final SellerVerificationRepository  verificationRepository;
    private final SellerNumberService          sellerNumberService;
    private final SellerMapper                 mapper;
    private final AuditLogService              auditLogService;
    private final SellerOwnershipPolicy        ownershipPolicy;

    @Override
    @Transactional
    public SellerResponse provision(UUID ownerUserId, String displayName) {
        if (sellerRepository.existsByOwnerUserId(ownerUserId)) {
            throw new SellerAlreadyExistsException(ownerUserId);
        }

        Seller seller = Seller.builder()
                .ownerUserId(ownerUserId)
                .sellerNumber("PENDING")
                .displayName(displayName)
                .build();
        seller = sellerRepository.save(seller);

        String sellerNumber = sellerNumberService.formatFromId(seller.getId());
        seller.setSellerNumber(sellerNumber);
        seller = sellerRepository.save(seller);

        auditLogService.log(AuditLogEntry.builder()
                .action(AuditAction.SELLER_REGISTERED)
                .entityType("SELLER")
                .entityPublicId(seller.getPublicId())
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());

        log.info("Seller provisioned: sellerNumber={}, ownerUserId={}", sellerNumber, ownerUserId);
        return mapper.toResponse(seller);
    }

    @Override
    public SellerDetailResponse getMySeller(UUID userId) {
        Seller seller = ownershipPolicy.resolveOwn(userId);
        return buildDetailResponse(seller);
    }

    @Override
    public SellerDetailResponse getSellerByPublicId(UUID publicId) {
        Seller seller = sellerRepository.findByPublicId(publicId)
                .orElseThrow(() -> new SellerNotFoundException(publicId));
        return buildDetailResponse(seller);
    }

    @Override
    @Transactional
    public SellerResponse updateMySeller(UUID userId, UpdateSellerRequest request) {
        Seller seller = ownershipPolicy.resolveOwn(userId);

        if (request.getDisplayName() != null) {
            seller.setDisplayName(request.getDisplayName());
        }
        seller = sellerRepository.save(seller);

        auditLogService.log(AuditLogEntry.builder()
                .action(AuditAction.SELLER_PROFILE_UPDATED)
                .actorPublicId(userId)
                .entityType("SELLER")
                .entityPublicId(seller.getPublicId())
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());

        return mapper.toResponse(seller);
    }

    @Override
    public Page<SellerSummaryResponse> searchSellers(SellerSearchRequest params, Pageable pageable) {
        return sellerRepository.findAll(SellerSpec.fromRequest(params), pageable)
                .map(mapper::toSummary);
    }

    @Override
    public List<SellerStatusHistoryResponse> getStatusHistory(UUID sellerPublicId) {
        Seller seller = sellerRepository.findByPublicId(sellerPublicId)
                .orElseThrow(() -> new SellerNotFoundException(sellerPublicId));
        return historyRepository.findBySellerIdOrderByCreatedAtDesc(seller.getId())
                .stream()
                .map(mapper::toStatusHistoryResponse)
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private SellerDetailResponse buildDetailResponse(Seller seller) {
        SellerBusiness business = businessRepository.findBySellerId(seller.getId()).orElse(null);
        List<SellerVerification> verifications = verificationRepository.findBySellerId(seller.getId());
        return mapper.toDetailResponse(seller, business, verifications);
    }
}

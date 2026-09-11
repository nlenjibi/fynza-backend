package ecommerce.modules.seller.service.impl;

import ecommerce.common.enums.SellerStatus;
import ecommerce.modules.audit.constant.AuditAction;
import ecommerce.modules.audit.dto.AuditLogEntry;
import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.seller.dto.request.SellerStatusRequest;
import ecommerce.modules.seller.dto.response.SellerResponse;
import ecommerce.modules.seller.entity.Seller;
import ecommerce.modules.seller.entity.SellerStatusHistory;
import ecommerce.modules.seller.exception.SellerNotFoundException;
import ecommerce.modules.seller.mapper.SellerMapper;
import ecommerce.modules.seller.repository.SellerRepository;
import ecommerce.modules.seller.repository.SellerStatusHistoryRepository;
import ecommerce.modules.seller.service.SellerStatusService;
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
public class SellerStatusServiceImpl implements SellerStatusService {

    private final SellerRepository                 sellerRepository;
    private final SellerStatusHistoryRepository    historyRepository;
    private final SellerMapper                     mapper;
    private final AuditLogService                  auditLogService;
    private final SellerStatusTransitionValidator  transitionValidator;

    @Override
    @Transactional
    public SellerResponse suspendSeller(UUID sellerPublicId, UUID actorId, SellerStatusRequest request) {
        return changeStatus(sellerPublicId, actorId, SellerStatus.SUSPENDED,
                request.getReason(), request.getExpiresAt(), AuditAction.SELLER_SUSPENDED);
    }

    @Override
    @Transactional
    public SellerResponse activateSeller(UUID sellerPublicId, UUID actorId) {
        return changeStatus(sellerPublicId, actorId, SellerStatus.ACTIVE,
                "Administrative reactivation", null, AuditAction.SELLER_REACTIVATED);
    }

    @Override
    @Transactional
    public SellerResponse approveSeller(UUID sellerPublicId, UUID actorId) {
        return changeStatus(sellerPublicId, actorId, SellerStatus.ACTIVE,
                "Application approved", null, AuditAction.SELLER_APPROVED);
    }

    @Override
    @Transactional
    public SellerResponse rejectSeller(UUID sellerPublicId, UUID actorId, SellerStatusRequest request) {
        return changeStatus(sellerPublicId, actorId, SellerStatus.REJECTED,
                request.getReason(), null, AuditAction.SELLER_REJECTED);
    }

    @Override
    @Transactional
    public SellerResponse blockSeller(UUID sellerPublicId, UUID actorId, SellerStatusRequest request) {
        return changeStatus(sellerPublicId, actorId, SellerStatus.BLOCKED,
                request.getReason(), null, AuditAction.SELLER_SUSPENDED);
    }

    @Override
    @Transactional
    public SellerResponse closeSeller(UUID sellerPublicId, UUID actorId, SellerStatusRequest request) {
        return changeStatus(sellerPublicId, actorId, SellerStatus.CLOSED,
                request.getReason(), null, AuditAction.SELLER_PROFILE_UPDATED);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private SellerResponse changeStatus(UUID sellerPublicId, UUID actorId, SellerStatus newStatus,
                                         String reason, java.time.Instant expiresAt, String auditAction) {
        Seller seller = sellerRepository.findByPublicId(sellerPublicId)
                .orElseThrow(() -> new SellerNotFoundException(sellerPublicId));

        SellerStatus previous = seller.getStatus();
        transitionValidator.validate(previous, newStatus);

        seller.setStatus(newStatus);
        sellerRepository.save(seller);

        historyRepository.save(SellerStatusHistory.builder()
                .sellerId(seller.getId())
                .previousStatus(previous)
                .newStatus(newStatus)
                .reason(reason)
                .changedBy(actorId)
                .expiresAt(expiresAt)
                .build());

        auditLogService.log(AuditLogEntry.builder()
                .action(auditAction)
                .actorPublicId(actorId)
                .entityType("SELLER")
                .entityPublicId(seller.getPublicId())
                .reason(reason)
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());

        log.info("Seller {} status changed: {} → {} by {}", seller.getSellerNumber(), previous, newStatus, actorId);
        return mapper.toResponse(seller);
    }
}

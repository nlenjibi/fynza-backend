package ecommerce.modules.store.service.impl;

import ecommerce.modules.audit.constant.AuditAction;
import ecommerce.modules.audit.dto.AuditLogEntry;
import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.store.dto.request.StoreStatusRequest;
import ecommerce.modules.store.dto.response.StoreResponse;
import ecommerce.modules.store.dto.response.StoreStatusHistoryResponse;
import ecommerce.modules.store.entity.Store;
import ecommerce.modules.store.entity.StoreStatusHistory;
import ecommerce.modules.store.exception.StoreNotFoundException;
import ecommerce.modules.store.mapper.StoreMapper;
import ecommerce.modules.store.repository.StoreRepository;
import ecommerce.modules.store.repository.StoreStatusHistoryRepository;
import ecommerce.modules.store.service.StoreStatusService;
import ecommerce.modules.store.validation.StoreStatusTransitionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreStatusServiceImpl implements StoreStatusService {

    private static final Map<ecommerce.modules.store.enums.StoreStatus, String> STATUS_AUDIT_ACTIONS = Map.of(
            ecommerce.modules.store.enums.StoreStatus.ACTIVE,         AuditAction.STORE_ACTIVATED,
            ecommerce.modules.store.enums.StoreStatus.PAUSED,         AuditAction.STORE_PAUSED,
            ecommerce.modules.store.enums.StoreStatus.SUSPENDED,      AuditAction.STORE_SUSPENDED,
            ecommerce.modules.store.enums.StoreStatus.CLOSED,         AuditAction.STORE_CLOSED,
            ecommerce.modules.store.enums.StoreStatus.ARCHIVED,       AuditAction.STORE_ARCHIVED,
            ecommerce.modules.store.enums.StoreStatus.PENDING_REVIEW, AuditAction.STORE_SUBMITTED_FOR_REVIEW
    );

    private final StoreRepository                storeRepository;
    private final StoreStatusHistoryRepository   historyRepository;
    private final StoreStatusTransitionValidator transitionValidator;
    private final StoreMapper                    mapper;
    private final AuditLogService                auditLogService;

    @Override
    @Transactional
    public StoreResponse changeStatus(UUID storePublicId, UUID actorUserId, StoreStatusRequest request) {
        Store store = storeRepository.findByPublicId(storePublicId)
                .orElseThrow(() -> new StoreNotFoundException(storePublicId));

        transitionValidator.validate(store.getStatus(), request.getStatus());
        ecommerce.modules.store.enums.StoreStatus previous = store.getStatus();
        store.setStatus(request.getStatus());
        storeRepository.save(store);

        historyRepository.save(StoreStatusHistory.builder()
                .storeId(store.getId())
                .previousStatus(previous)
                .newStatus(request.getStatus())
                .reason(request.getReason())
                .changedBy(actorUserId)
                .expiresAt(request.getExpiresAt())
                .build());

        String auditAction = STATUS_AUDIT_ACTIONS.getOrDefault(request.getStatus(), AuditAction.STORE_UPDATED);
        auditLogService.log(AuditLogEntry.builder()
                .action(auditAction)
                .entityType("STORE")
                .entityPublicId(store.getPublicId())
                .actorPublicId(actorUserId)
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());

        log.info("Store status changed: publicId={}, {} → {}", storePublicId, previous, request.getStatus());
        return mapper.toResponse(store);
    }

    @Override
    public List<StoreStatusHistoryResponse> getHistory(UUID storePublicId) {
        Store store = storeRepository.findByPublicId(storePublicId)
                .orElseThrow(() -> new StoreNotFoundException(storePublicId));
        return historyRepository.findByStoreIdOrderByCreatedAtDesc(store.getId())
                .stream().map(mapper::toStatusHistoryResponse).toList();
    }
}

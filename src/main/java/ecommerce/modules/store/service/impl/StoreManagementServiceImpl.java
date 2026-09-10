package ecommerce.modules.store.service.impl;

import ecommerce.modules.audit.constant.AuditAction;
import ecommerce.modules.audit.dto.AuditLogEntry;
import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.store.dto.request.*;
import ecommerce.modules.store.dto.response.*;
import ecommerce.modules.store.entity.*;
import ecommerce.modules.store.enums.StoreStatus;
import ecommerce.modules.store.enums.StoreVisibility;
import ecommerce.modules.store.exception.StoreNotFoundException;
import ecommerce.modules.store.mapper.StoreMapper;
import ecommerce.modules.store.policy.StoreOwnershipPolicy;
import ecommerce.modules.store.repository.*;
import ecommerce.modules.store.service.StoreManagementService;
import ecommerce.modules.store.service.StoreSlugService;
import ecommerce.modules.store.spec.StoreSummarySpec;
import ecommerce.modules.store.validation.StoreStatusTransitionValidator;
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
public class StoreManagementServiceImpl implements StoreManagementService {

    private final StoreRepository                storeRepository;
    private final StoreSettingRepository         settingRepository;
    private final StorePolicyRepository          policyRepository;
    private final StoreStatusHistoryRepository   statusHistoryRepository;
    private final StoreSummaryViewRepository     summaryViewRepository;
    private final StoreOwnershipPolicy           ownershipPolicy;
    private final StoreSlugService               slugService;
    private final StoreStatusTransitionValidator transitionValidator;
    private final StoreMapper                    mapper;
    private final AuditLogService                auditLogService;

    @Override
    @Transactional
    public StoreDetailResponse createStore(UUID actorUserId, CreateStoreRequest request) {
        var seller = ownershipPolicy.resolveSeller(actorUserId);
        if (storeRepository.existsBySellerIdAndStatusNot(seller.getId(), StoreStatus.ARCHIVED)) {
            throw new ecommerce.modules.store.exception.StoreAlreadyExistsException(seller.getPublicId());
        }

        String slug = (request.getSlug() != null && !request.getSlug().isBlank())
                ? request.getSlug()
                : slugService.generateSlug(request.getStoreName());
        slugService.validateAndReserve(slug, 0L); // placeholder id; replaced after save

        Store store = Store.builder()
                .sellerId(seller.getId())
                .storeName(request.getStoreName())
                .slug(slug)
                .description(request.getDescription())
                .businessEmail(request.getBusinessEmail())
                .businessPhone(request.getBusinessPhone())
                .website(request.getWebsite())
                .status(StoreStatus.DRAFT)
                .visibility(StoreVisibility.PRIVATE)
                .isActive(true)
                .build();
        storeRepository.save(store);

        StoreSetting setting = StoreSetting.builder()
                .store(store)
                .build();
        settingRepository.save(setting);

        auditLogService.log(AuditLogEntry.builder()
                .action(AuditAction.STORE_CREATED)
                .entityType("STORE")
                .entityPublicId(store.getPublicId())
                .actorPublicId(actorUserId)
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());

        log.info("Store created: slug={}, sellerId={}", store.getSlug(), store.getSellerId());
        return mapper.toDetailResponse(store, setting, List.of());
    }

    @Override
    public StoreDetailResponse getMyStore(UUID actorUserId) {
        Store store = ownershipPolicy.resolveOwnStore(actorUserId);
        return buildDetail(store);
    }

    @Override
    public StoreDetailResponse getStoreByPublicId(UUID publicId) {
        Store store = storeRepository.findByPublicId(publicId)
                .orElseThrow(() -> new StoreNotFoundException(publicId));
        return buildDetail(store);
    }

    @Override
    public StoreSummaryResponse getStoreBySlug(String slug) {
        return summaryViewRepository.findBySlug(slug)
                .map(mapper::toSummary)
                .orElseThrow(() -> new StoreNotFoundException("Store not found for slug: " + slug));
    }

    @Override
    @Transactional
    public StoreResponse updateStore(UUID actorUserId, UpdateStoreRequest request) {
        Store store = ownershipPolicy.resolveOwnStore(actorUserId);
        applyUpdates(store, request);
        storeRepository.save(store);

        auditLogService.log(AuditLogEntry.builder()
                .action(AuditAction.STORE_UPDATED)
                .entityType("STORE")
                .entityPublicId(store.getPublicId())
                .actorPublicId(actorUserId)
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());

        return mapper.toResponse(store);
    }

    @Override
    @Transactional
    public StoreResponse updateVisibility(UUID actorUserId, StoreVisibilityRequest request) {
        Store store = ownershipPolicy.resolveOwnStore(actorUserId);
        store.setVisibility(request.getVisibility());
        storeRepository.save(store);

        auditLogService.log(AuditLogEntry.builder()
                .action(AuditAction.STORE_VISIBILITY_CHANGED)
                .entityType("STORE")
                .entityPublicId(store.getPublicId())
                .actorPublicId(actorUserId)
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());

        return mapper.toResponse(store);
    }

    @Override
    @Transactional
    public StoreResponse submitForReview(UUID actorUserId) {
        Store store = ownershipPolicy.resolveOwnStore(actorUserId);
        transitionValidator.validate(store.getStatus(), StoreStatus.PENDING_REVIEW);
        StoreStatus previous = store.getStatus();
        store.setStatus(StoreStatus.PENDING_REVIEW);
        storeRepository.save(store);

        statusHistoryRepository.save(StoreStatusHistory.builder()
                .storeId(store.getId())
                .previousStatus(previous)
                .newStatus(StoreStatus.PENDING_REVIEW)
                .changedBy(actorUserId)
                .build());

        auditLogService.log(AuditLogEntry.builder()
                .action(AuditAction.STORE_SUBMITTED_FOR_REVIEW)
                .entityType("STORE")
                .entityPublicId(store.getPublicId())
                .actorPublicId(actorUserId)
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());

        return mapper.toResponse(store);
    }

    @Override
    public Page<StoreSummaryResponse> searchStores(StoreSearchRequest params, Pageable pageable) {
        return summaryViewRepository.findAll(StoreSummarySpec.fromRequest(params), pageable)
                .map(mapper::toSummary);
    }

    @Override
    public List<StoreStatusHistoryResponse> getStatusHistory(UUID storePublicId) {
        Store store = storeRepository.findByPublicId(storePublicId)
                .orElseThrow(() -> new StoreNotFoundException(storePublicId));
        return statusHistoryRepository.findByStoreIdOrderByCreatedAtDesc(store.getId())
                .stream().map(mapper::toStatusHistoryResponse).toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private StoreDetailResponse buildDetail(Store store) {
        StoreSetting setting = settingRepository.findByStore_Id(store.getId()).orElse(null);
        List<StorePolicy> policies = policyRepository.findByStoreIdAndIsActiveTrue(store.getId());
        return mapper.toDetailResponse(store, setting, policies);
    }

    private void applyUpdates(Store store, UpdateStoreRequest request) {
        if (request.getStoreName() != null) store.setStoreName(request.getStoreName());
        if (request.getDescription() != null) store.setDescription(request.getDescription());
        if (request.getLogoMediaId() != null) store.setLogoMediaId(request.getLogoMediaId());
        if (request.getBannerMediaId() != null) store.setBannerMediaId(request.getBannerMediaId());
        if (request.getBusinessEmail() != null) store.setBusinessEmail(request.getBusinessEmail());
        if (request.getBusinessPhone() != null) store.setBusinessPhone(request.getBusinessPhone());
        if (request.getWebsite() != null) store.setWebsite(request.getWebsite());
    }
}

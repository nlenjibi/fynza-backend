package ecommerce.modules.store.service.impl;

import ecommerce.modules.audit.constant.AuditAction;
import ecommerce.modules.audit.dto.AuditLogEntry;
import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.store.dto.request.StorePolicyRequest;
import ecommerce.modules.store.dto.response.StorePolicyResponse;
import ecommerce.modules.store.entity.Store;
import ecommerce.modules.store.entity.StorePolicy;
import ecommerce.modules.store.enums.StorePolicyStatus;
import ecommerce.modules.store.exception.StorePolicyNotFoundException;
import ecommerce.modules.store.mapper.StoreMapper;
import ecommerce.modules.store.policy.StoreOwnershipPolicy;
import ecommerce.modules.store.repository.StorePolicyRepository;
import ecommerce.modules.store.service.StorePolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StorePolicyServiceImpl implements StorePolicyService {

    private final StoreOwnershipPolicy  ownershipPolicy;
    private final StorePolicyRepository policyRepository;
    private final StoreMapper           mapper;
    private final AuditLogService       auditLogService;

    @Override
    public List<StorePolicyResponse> getPolicies(UUID actorUserId) {
        Store store = ownershipPolicy.resolveOwnStore(actorUserId);
        return policyRepository.findByStoreIdAndIsActiveTrue(store.getId())
                .stream().map(mapper::toPolicyResponse).toList();
    }

    @Override
    @Transactional
    public StorePolicyResponse createPolicy(UUID actorUserId, StorePolicyRequest request) {
        Store store = ownershipPolicy.resolveOwnStore(actorUserId);
        StorePolicy policy = StorePolicy.builder()
                .storeId(store.getId())
                .type(request.getType())
                .title(request.getTitle())
                .content(request.getContent())
                .status(request.getStatus() != null ? request.getStatus() : StorePolicyStatus.DRAFT)
                .effectiveFrom(request.getEffectiveFrom())
                .build();
        policyRepository.save(policy);

        auditLogService.log(AuditLogEntry.builder()
                .action(AuditAction.STORE_POLICY_CREATED)
                .entityType("STORE_POLICY")
                .entityPublicId(policy.getPublicId())
                .actorPublicId(actorUserId)
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());

        return mapper.toPolicyResponse(policy);
    }

    @Override
    @Transactional
    public StorePolicyResponse updatePolicy(UUID actorUserId, UUID policyPublicId, StorePolicyRequest request) {
        Store store = ownershipPolicy.resolveOwnStore(actorUserId);
        StorePolicy policy = policyRepository.findByPublicId(policyPublicId)
                .orElseThrow(() -> new StorePolicyNotFoundException(policyPublicId));

        if (!policy.getStoreId().equals(store.getId())) {
            throw new ecommerce.common.exception.ForbiddenException("Policy does not belong to your store");
        }

        if (request.getTitle() != null)         policy.setTitle(request.getTitle());
        if (request.getContent() != null)        policy.setContent(request.getContent());
        if (request.getStatus() != null)         policy.setStatus(request.getStatus());
        if (request.getEffectiveFrom() != null)  policy.setEffectiveFrom(request.getEffectiveFrom());
        policy.setVersion(policy.getVersion() + 1);
        policyRepository.save(policy);

        auditLogService.log(AuditLogEntry.builder()
                .action(AuditAction.STORE_POLICY_UPDATED)
                .entityType("STORE_POLICY")
                .entityPublicId(policy.getPublicId())
                .actorPublicId(actorUserId)
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());

        return mapper.toPolicyResponse(policy);
    }

    @Override
    @Transactional
    public void deletePolicy(UUID actorUserId, UUID policyPublicId) {
        Store store = ownershipPolicy.resolveOwnStore(actorUserId);
        StorePolicy policy = policyRepository.findByPublicId(policyPublicId)
                .orElseThrow(() -> new StorePolicyNotFoundException(policyPublicId));

        if (!policy.getStoreId().equals(store.getId())) {
            throw new ecommerce.common.exception.ForbiddenException("Policy does not belong to your store");
        }

        policy.setIsActive(false);
        policyRepository.save(policy);

        auditLogService.log(AuditLogEntry.builder()
                .action(AuditAction.STORE_POLICY_DELETED)
                .entityType("STORE_POLICY")
                .entityPublicId(policy.getPublicId())
                .actorPublicId(actorUserId)
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());
    }
}

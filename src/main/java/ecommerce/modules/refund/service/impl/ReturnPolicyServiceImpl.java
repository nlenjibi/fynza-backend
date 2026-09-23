package ecommerce.modules.refund.service.impl;

import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.refund.dto.CreateReturnPolicyRequest;
import ecommerce.modules.refund.dto.ReturnPolicyResponse;
import ecommerce.modules.refund.entity.ReturnPolicy;
import ecommerce.modules.refund.enums.RefundMethod;
import ecommerce.modules.refund.enums.ReturnPolicyScope;
import ecommerce.modules.refund.enums.ReturnReason;
import ecommerce.modules.refund.enums.ReturnShippingResponsibility;
import ecommerce.modules.refund.repository.ReturnPolicyExclusionRepository;
import ecommerce.modules.refund.repository.ReturnPolicyRepository;
import ecommerce.modules.refund.service.ReturnPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReturnPolicyServiceImpl implements ReturnPolicyService {

    private static final int DEFAULT_RETURN_WINDOW_DAYS = 14;

    private final ReturnPolicyRepository policyRepository;
    private final ReturnPolicyExclusionRepository exclusionRepository;

    @Override
    public ReturnPolicy resolvePolicy(UUID productId, UUID categoryId, UUID storeId) {
        if (productId != null) {
            var product = policyRepository.findByScopeAndProductIdAndIsActiveTrue(ReturnPolicyScope.PRODUCT, productId);
            if (product.isPresent()) return product.get();
        }
        if (categoryId != null) {
            var category = policyRepository.findByScopeAndCategoryIdAndIsActiveTrue(ReturnPolicyScope.CATEGORY, categoryId);
            if (category.isPresent()) return category.get();
        }
        if (storeId != null) {
            var store = policyRepository.findByScopeAndStoreIdAndIsActiveTrue(ReturnPolicyScope.STORE, storeId);
            if (store.isPresent()) return store.get();
        }
        return policyRepository.findByScopeAndIsActiveTrue(ReturnPolicyScope.PLATFORM)
                .orElseGet(this::systemDefaultPolicy);
    }

    @Override
    public ReturnPolicyResponse getPlatformPolicy() {
        ReturnPolicy policy = policyRepository.findByScopeAndIsActiveTrue(ReturnPolicyScope.PLATFORM)
                .orElseGet(this::systemDefaultPolicy);
        return toResponse(policy);
    }

    @Override
    public ReturnPolicyResponse getStorePolicy(UUID storeId) {
        ReturnPolicy policy = policyRepository.findByScopeAndStoreIdAndIsActiveTrue(ReturnPolicyScope.STORE, storeId)
                .orElseGet(this::systemDefaultPolicy);
        return toResponse(policy);
    }

    @Override
    public ReturnPolicyResponse getPolicy(UUID policyPublicId) {
        ReturnPolicy policy = policyRepository.findByPublicId(policyPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Return policy not found: " + policyPublicId));
        return toResponse(policy);
    }

    @Override
    @Transactional
    public ReturnPolicyResponse createPolicy(CreateReturnPolicyRequest request) {
        ReturnPolicy policy = ReturnPolicy.builder()
                .name(request.getName())
                .scope(request.getScope())
                .storeId(request.getStoreId())
                .productId(request.getProductId())
                .categoryId(request.getCategoryId())
                .returnWindowDays(request.getReturnWindowDays())
                .isReturnable(request.getIsReturnable())
                .conditionRequired(request.getConditionRequired())
                .eligibleReasons(serializeReasons(request.getEligibleReasons()))
                .returnShippingResponsibility(
                        request.getReturnShippingResponsibility() != null
                                ? request.getReturnShippingResponsibility()
                                : ReturnShippingResponsibility.CUSTOMER)
                .restockingFeePercent(request.getRestockingFeePercent())
                .refundMethod(request.getRefundMethod() != null
                        ? request.getRefundMethod()
                        : RefundMethod.ORIGINAL_PAYMENT)
                .build();
        policy = policyRepository.save(policy);
        log.info("Return policy created: {} (scope={})", policy.getName(), policy.getScope());
        return toResponse(policy);
    }

    @Override
    @Transactional
    public ReturnPolicyResponse updatePolicy(UUID policyPublicId, CreateReturnPolicyRequest request) {
        ReturnPolicy policy = policyRepository.findByPublicId(policyPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Return policy not found: " + policyPublicId));
        policy.setName(request.getName());
        policy.setReturnWindowDays(request.getReturnWindowDays());
        policy.setIsReturnable(request.getIsReturnable());
        policy.setConditionRequired(request.getConditionRequired());
        policy.setEligibleReasons(serializeReasons(request.getEligibleReasons()));
        if (request.getReturnShippingResponsibility() != null) {
            policy.setReturnShippingResponsibility(request.getReturnShippingResponsibility());
        }
        policy.setRestockingFeePercent(request.getRestockingFeePercent());
        if (request.getRefundMethod() != null) {
            policy.setRefundMethod(request.getRefundMethod());
        }
        policy = policyRepository.save(policy);
        log.info("Return policy updated: {}", policy.getPublicId());
        return toResponse(policy);
    }

    @Override
    @Transactional
    public ReturnPolicyResponse togglePolicy(UUID policyPublicId) {
        ReturnPolicy policy = policyRepository.findByPublicId(policyPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Return policy not found: " + policyPublicId));
        policy.setIsActive(!policy.getIsActive());
        policy = policyRepository.save(policy);
        log.info("Return policy {} toggled to {}", policy.getPublicId(), policy.getIsActive());
        return toResponse(policy);
    }

    @Override
    public boolean isExcluded(ReturnPolicy policy, UUID productId, UUID categoryId) {
        if (policy.getPublicId() == null) return false;
        if (productId != null && exclusionRepository.existsByPolicyIdAndReferenceId(policy.getPublicId(), productId)) {
            return true;
        }
        return categoryId != null && exclusionRepository.existsByPolicyIdAndReferenceId(policy.getPublicId(), categoryId);
    }

    private ReturnPolicy systemDefaultPolicy() {
        return ReturnPolicy.builder()
                .returnWindowDays(DEFAULT_RETURN_WINDOW_DAYS)
                .isReturnable(true)
                .conditionRequired(false)
                .returnShippingResponsibility(ReturnShippingResponsibility.CUSTOMER)
                .refundMethod(RefundMethod.ORIGINAL_PAYMENT)
                .isActive(true)
                .build();
    }

    private String serializeReasons(List<ReturnReason> reasons) {
        if (reasons == null || reasons.isEmpty()) return null;
        return reasons.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    private List<ReturnReason> deserializeReasons(String serialized) {
        if (serialized == null || serialized.isBlank()) return Collections.emptyList();
        return Arrays.stream(serialized.split(","))
                .map(String::trim)
                .map(ReturnReason::valueOf)
                .collect(Collectors.toList());
    }

    private ReturnPolicyResponse toResponse(ReturnPolicy p) {
        List<ReturnReason> reasons = deserializeReasons(p.getEligibleReasons());
        return ReturnPolicyResponse.builder()
                .publicId(p.getPublicId())
                .name(p.getName())
                .scope(p.getScope())
                .storeId(p.getStoreId())
                .productId(p.getProductId())
                .categoryId(p.getCategoryId())
                .returnWindowDays(p.getReturnWindowDays())
                .isReturnable(p.getIsReturnable())
                .conditionRequired(p.getConditionRequired())
                .eligibleReasons(reasons.isEmpty() ? List.of(ReturnReason.values()) : reasons)
                .returnShippingResponsibility(p.getReturnShippingResponsibility())
                .restockingFeePercent(p.getRestockingFeePercent())
                .refundMethod(p.getRefundMethod())
                .isActive(p.getIsActive())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}

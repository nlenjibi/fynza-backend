package ecommerce.modules.refund.service;

import ecommerce.modules.refund.dto.CreateReturnPolicyRequest;
import ecommerce.modules.refund.dto.ReturnPolicyResponse;
import ecommerce.modules.refund.entity.ReturnPolicy;

import java.util.UUID;

public interface ReturnPolicyService {

    /**
     * Resolves the most specific active policy for a given context.
     * Precedence: product → category → store → platform → system defaults.
     */
    ReturnPolicy resolvePolicy(UUID productId, UUID categoryId, UUID storeId);

    ReturnPolicyResponse getPlatformPolicy();

    ReturnPolicyResponse getStorePolicy(UUID storeId);

    ReturnPolicyResponse getPolicy(UUID policyPublicId);

    ReturnPolicyResponse createPolicy(CreateReturnPolicyRequest request);

    ReturnPolicyResponse updatePolicy(UUID policyPublicId, CreateReturnPolicyRequest request);

    ReturnPolicyResponse togglePolicy(UUID policyPublicId);

    boolean isExcluded(ReturnPolicy policy, UUID productId, UUID categoryId);
}

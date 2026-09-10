package ecommerce.modules.store.service;

import ecommerce.modules.store.dto.request.StorePolicyRequest;
import ecommerce.modules.store.dto.response.StorePolicyResponse;

import java.util.List;
import java.util.UUID;

public interface StorePolicyService {

    List<StorePolicyResponse> getPolicies(UUID actorUserId);

    StorePolicyResponse createPolicy(UUID actorUserId, StorePolicyRequest request);

    StorePolicyResponse updatePolicy(UUID actorUserId, UUID policyPublicId, StorePolicyRequest request);

    void deletePolicy(UUID actorUserId, UUID policyPublicId);
}

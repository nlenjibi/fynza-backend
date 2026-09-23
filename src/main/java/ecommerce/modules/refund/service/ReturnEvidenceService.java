package ecommerce.modules.refund.service;

import ecommerce.modules.refund.dto.AddReturnEvidenceRequest;
import ecommerce.modules.refund.dto.ReturnAuditResponse;
import ecommerce.modules.refund.dto.ReturnEvidenceResponse;

import java.util.List;
import java.util.UUID;

public interface ReturnEvidenceService {

    ReturnEvidenceResponse addEvidence(UUID returnPublicId, AddReturnEvidenceRequest request, UUID uploadedBy);

    List<ReturnEvidenceResponse> getEvidence(UUID returnPublicId);

    List<ReturnAuditResponse> getAuditTrail(UUID returnPublicId);
}

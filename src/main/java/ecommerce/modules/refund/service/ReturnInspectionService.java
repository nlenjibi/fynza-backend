package ecommerce.modules.refund.service;

import ecommerce.modules.refund.dto.CompleteInspectionRequest;
import ecommerce.modules.refund.dto.ReturnDispositionResponse;
import ecommerce.modules.refund.dto.ReturnInspectionResponse;
import ecommerce.modules.refund.dto.ReturnResponse;

import java.util.List;
import java.util.UUID;

public interface ReturnInspectionService {

    ReturnResponse completeInspection(UUID returnPublicId, CompleteInspectionRequest request, UUID inspectedBy);

    List<ReturnInspectionResponse> getInspections(UUID returnPublicId);

    List<ReturnDispositionResponse> getDispositions(UUID returnPublicId);
}

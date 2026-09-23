package ecommerce.modules.refund.service;

import ecommerce.modules.refund.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ReturnService {

    ReturnEligibilityResult checkEligibility(UUID orderId, UUID customerId);

    ReturnResponse createReturn(CreateReturnRequest request, UUID customerId);

    ReturnResponse getReturn(UUID returnPublicId, UUID requestingUserId);

    Page<ReturnResponse> getReturnsByCustomer(UUID customerId, Pageable pageable);

    List<ReturnResponse> getReturnsByOrder(UUID orderId);

    Page<ReturnResponse> getReturnsBySeller(UUID sellerId, Pageable pageable);

    Page<ReturnResponse> getAllReturns(Pageable pageable);

    ReturnResponse cancelReturn(UUID returnPublicId, UUID customerId);

    ReturnResponse approveReturn(UUID returnPublicId, UUID approvedBy, String adminNote);

    ReturnResponse rejectReturn(UUID returnPublicId, UUID rejectedBy, String rejectionReason);

    ReturnResponse markUnderReview(UUID returnPublicId, UUID reviewedBy);

    ReturnResponse escalateReturn(UUID returnPublicId, UUID escalatedBy, String reason);

    ReturnResponse startInspection(UUID returnPublicId, UUID inspectedBy);

    ReturnResponse initiateReturnShipment(UUID returnPublicId, InitiateReturnShipmentRequest request, UUID initiatedBy);

    ReturnResponse markInTransit(UUID returnPublicId, UUID triggeredBy);

    ReturnResponse markReceived(UUID returnPublicId, UUID triggeredBy);
}

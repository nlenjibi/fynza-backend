package ecommerce.modules.refund.service;

import ecommerce.modules.refund.dto.ReturnReconciliationResponse;
import ecommerce.modules.refund.dto.ReturnRefundResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReturnRefundService {

    ReturnRefundResponse requestRefund(UUID returnPublicId, UUID requestedBy);

    Optional<ReturnRefundResponse> getRefund(UUID returnPublicId);

    ReturnReconciliationResponse reconcile(UUID returnPublicId, UUID reconciledBy);

    List<ReturnReconciliationResponse> getReconciliations(UUID returnPublicId);
}

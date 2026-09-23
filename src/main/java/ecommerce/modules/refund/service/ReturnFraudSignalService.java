package ecommerce.modules.refund.service;

import ecommerce.modules.refund.dto.ReturnFraudSignalResponse;
import ecommerce.modules.refund.entity.Return;

import java.util.List;
import java.util.UUID;

public interface ReturnFraudSignalService {

    void analyzeAndRecord(Return ret);

    List<ReturnFraudSignalResponse> getSignals(UUID returnPublicId);
}

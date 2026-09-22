package ecommerce.modules.payout.service;

import ecommerce.modules.payout.dto.response.PayoutResponse;

import java.util.UUID;

public interface PayoutAdminService {

    PayoutResponse approvePayout(UUID payoutPublicId);

    PayoutResponse rejectPayout(UUID payoutPublicId, String reason);

    PayoutResponse holdPayout(UUID payoutPublicId, String reason);

    PayoutResponse retryPayout(UUID payoutPublicId);

    PayoutResponse cancelPayout(UUID payoutPublicId);
}

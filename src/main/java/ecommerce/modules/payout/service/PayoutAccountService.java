package ecommerce.modules.payout.service;

import ecommerce.modules.payout.dto.request.AddPayoutAccountRequest;
import ecommerce.modules.payout.dto.request.UpdatePayoutAccountRequest;
import ecommerce.modules.payout.dto.response.PayoutAccountResponse;

import java.util.List;
import java.util.UUID;

public interface PayoutAccountService {

    PayoutAccountResponse addAccount(Long sellerId, AddPayoutAccountRequest request);

    PayoutAccountResponse updateAccount(Long sellerId, UUID accountPublicId, UpdatePayoutAccountRequest request);

    void removeAccount(Long sellerId, UUID accountPublicId);

    PayoutAccountResponse setDefault(Long sellerId, UUID accountPublicId);

    PayoutAccountResponse verifyAccount(UUID accountPublicId);

    List<PayoutAccountResponse> listAccounts(Long sellerId);
}

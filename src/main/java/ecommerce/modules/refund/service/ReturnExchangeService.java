package ecommerce.modules.refund.service;

import ecommerce.modules.refund.dto.RequestExchangeRequest;
import ecommerce.modules.refund.dto.ReturnExchangeResponse;
import ecommerce.modules.refund.enums.ExchangeStatus;

import java.util.Optional;
import java.util.UUID;

public interface ReturnExchangeService {

    ReturnExchangeResponse requestExchange(UUID returnPublicId, RequestExchangeRequest request, UUID requestedBy);

    Optional<ReturnExchangeResponse> getExchange(UUID returnPublicId);

    ReturnExchangeResponse updateExchangeStatus(UUID returnPublicId, ExchangeStatus newStatus, UUID updatedBy);
}

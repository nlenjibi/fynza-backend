package ecommerce.modules.store.service;

import ecommerce.modules.store.dto.request.StoreStatusRequest;
import ecommerce.modules.store.dto.response.StoreResponse;
import ecommerce.modules.store.dto.response.StoreStatusHistoryResponse;

import java.util.List;
import java.util.UUID;

public interface StoreStatusService {

    StoreResponse changeStatus(UUID storePublicId, UUID actorUserId, StoreStatusRequest request);

    List<StoreStatusHistoryResponse> getHistory(UUID storePublicId);
}

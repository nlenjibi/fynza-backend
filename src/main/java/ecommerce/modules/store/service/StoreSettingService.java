package ecommerce.modules.store.service;

import ecommerce.modules.store.dto.request.StoreSettingRequest;
import ecommerce.modules.store.dto.response.StoreSettingResponse;

import java.util.UUID;

public interface StoreSettingService {

    StoreSettingResponse getSettings(UUID actorUserId);

    StoreSettingResponse updateSettings(UUID actorUserId, StoreSettingRequest request);
}

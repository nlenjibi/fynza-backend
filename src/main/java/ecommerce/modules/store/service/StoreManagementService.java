package ecommerce.modules.store.service;

import ecommerce.modules.store.dto.request.*;
import ecommerce.modules.store.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface StoreManagementService {

    StoreDetailResponse createStore(UUID actorUserId, CreateStoreRequest request);

    StoreDetailResponse getMyStore(UUID actorUserId);

    StoreDetailResponse getStoreByPublicId(UUID publicId);

    StoreSummaryResponse getStoreBySlug(String slug);

    StoreResponse updateStore(UUID actorUserId, UpdateStoreRequest request);

    StoreResponse updateVisibility(UUID actorUserId, StoreVisibilityRequest request);

    StoreResponse submitForReview(UUID actorUserId);

    Page<StoreSummaryResponse> searchStores(StoreSearchRequest params, Pageable pageable);

    List<StoreStatusHistoryResponse> getStatusHistory(UUID storePublicId);
}

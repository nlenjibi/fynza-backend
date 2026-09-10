package ecommerce.modules.category.service;

import ecommerce.modules.category.dto.request.CategoryStatusRequest;
import ecommerce.modules.category.dto.response.CategoryResponse;
import ecommerce.modules.category.dto.response.CategoryStatusHistoryResponse;

import java.util.List;
import java.util.UUID;

public interface CategoryStatusService {

    CategoryResponse changeStatus(UUID categoryPublicId, CategoryStatusRequest request, UUID actorUserId);

    List<CategoryStatusHistoryResponse> getStatusHistory(UUID categoryPublicId);
}

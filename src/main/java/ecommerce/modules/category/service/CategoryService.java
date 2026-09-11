package ecommerce.modules.category.service;

import ecommerce.modules.category.dto.request.CategorySearchRequest;
import ecommerce.modules.category.dto.request.CreateCategoryRequest;
import ecommerce.modules.category.dto.request.MoveCategoryRequest;
import ecommerce.modules.category.dto.request.UpdateCategoryRequest;
import ecommerce.modules.category.dto.response.CategoryDetailResponse;
import ecommerce.modules.category.dto.response.CategorySummaryResponse;
import ecommerce.modules.category.dto.response.CategoryTreeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    CategoryDetailResponse createCategory(CreateCategoryRequest request, UUID actorUserId);

    CategoryDetailResponse updateCategory(UUID categoryPublicId, UpdateCategoryRequest request, UUID actorUserId);

    void moveCategory(UUID categoryPublicId, MoveCategoryRequest request, UUID actorUserId);

    void deleteCategory(UUID categoryPublicId, UUID actorUserId);

    CategoryDetailResponse getCategoryByPublicId(UUID publicId);

    CategoryDetailResponse getCategoryBySlug(String slug);

    List<CategorySummaryResponse> getRootCategories(Long taxonomyId);

    List<CategorySummaryResponse> getChildren(UUID parentPublicId);

    List<CategoryTreeResponse> getCategoryTree(Long taxonomyId);

    Page<CategorySummaryResponse> searchCategories(CategorySearchRequest request, Pageable pageable);
}

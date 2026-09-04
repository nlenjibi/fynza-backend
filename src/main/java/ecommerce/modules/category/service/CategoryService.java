package ecommerce.modules.category.service;

import ecommerce.modules.category.dto.CategoryCreateRequest;
import ecommerce.modules.category.dto.CategoryResponse;
import ecommerce.modules.category.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface CategoryService {

    List<CategoryResponse> findAll();

    List<CategoryResponse> findActiveCategories();

    CategoryResponse findById(UUID id);

    List<CategoryResponse> findTree();

    CategoryResponse create(CategoryCreateRequest request);

    CategoryResponse update(UUID id, CategoryCreateRequest request);

    void delete(UUID id);

    CategoryResponse updateStatus(UUID id, Boolean isActive);

    Map<String, Object> getCategoryStats();

    CategoryResponse mapToResponse(Category category);

    Page<CategoryResponse> getAllCategories(Pageable pageable);

    CategoryResponse getCategoryById(UUID id);
}

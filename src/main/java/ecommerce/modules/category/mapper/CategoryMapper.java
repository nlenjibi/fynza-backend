package ecommerce.modules.category.mapper;

import ecommerce.modules.category.dto.CategoryCreateRequest;
import ecommerce.modules.category.dto.CategoryResponse;
import ecommerce.modules.category.entity.Category;

public interface CategoryMapper {
    CategoryResponse toSimpleResponse(Category category);
    Category toEntityFromRequest(CategoryCreateRequest request);
}

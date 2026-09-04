package ecommerce.modules.category.mapper;

import ecommerce.modules.category.dto.CategoryCreateRequest;
import ecommerce.modules.category.dto.CategoryResponse;
import ecommerce.modules.category.dto.CategoryUpdateRequest;
import ecommerce.modules.category.entity.Category;
import org.mapstruct.*;

import java.util.UUID;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = false))
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "featured", expression = "java(request.getFeatured() != null ? request.getFeatured() : false)")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "image", expression = "java(request.getImage() != null ? request.getImage() : null)")
    @Mapping(target = "parentCategory", expression = "java(mapParentCategory(request.getParentCategoryId()))")
    Category toEntity(CategoryCreateRequest request);

    @Mapping(target = "parentCategoryId", source = "parentCategory.id")
    CategoryResponse toSimpleResponse(Category category);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "image", expression = "java(request.getImage() != null ? request.getImage() : null)")
    @Mapping(target = "parentCategory", expression = "java(mapParentCategory(request.getParentCategoryId()))")
    void updateEntityFromRequest(CategoryUpdateRequest request, @MappingTarget Category category);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "image", expression = "java(request.getImage() != null ? request.getImage() : null)")
    @Mapping(target = "parentCategory", expression = "java(mapParentCategory(request.getParentCategoryId()))")
    void updateEntityFromRequest(CategoryCreateRequest request, @MappingTarget Category category);

    default Category mapParentCategory(UUID parentId) {
        if (parentId == null) {
            return null;
        }
        return Category.builder().id(parentId).build();
    }

    default Category toEntityFromRequest(CategoryCreateRequest request) {
        return toEntity(request);
    }
}

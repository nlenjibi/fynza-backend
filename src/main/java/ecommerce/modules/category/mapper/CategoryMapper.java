package ecommerce.modules.category.mapper;

import ecommerce.modules.category.dto.response.*;
import ecommerce.modules.category.entity.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category) {
        UUID parentPublicId = category.getParentCategory() != null
                ? category.getParentCategory().getPublicId() : null;
        return CategoryResponse.builder()
                .publicId(category.getPublicId())
                .taxonomyId(category.getTaxonomyId())
                .parentCategoryPublicId(parentPublicId)
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .status(category.getStatus())
                .visibility(category.getVisibility())
                .sortOrder(category.getSortOrder())
                .mediaId(category.getMediaId())
                .isActive(category.getIsActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    public CategoryDetailResponse toDetailResponse(Category category,
                                                    List<AttributeDefinitionResponse> attributes,
                                                    List<CategorySummaryResponse> children) {
        UUID parentPublicId = category.getParentCategory() != null
                ? category.getParentCategory().getPublicId() : null;
        return CategoryDetailResponse.builder()
                .publicId(category.getPublicId())
                .taxonomyId(category.getTaxonomyId())
                .parentCategoryPublicId(parentPublicId)
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .status(category.getStatus())
                .visibility(category.getVisibility())
                .sortOrder(category.getSortOrder())
                .mediaId(category.getMediaId())
                .isActive(category.getIsActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .attributes(attributes)
                .children(children)
                .build();
    }

    public CategoryTreeResponse toTreeResponse(Category category, List<CategoryTreeResponse> children) {
        return CategoryTreeResponse.builder()
                .publicId(category.getPublicId())
                .name(category.getName())
                .slug(category.getSlug())
                .mediaId(category.getMediaId())
                .status(category.getStatus())
                .visibility(category.getVisibility())
                .sortOrder(category.getSortOrder())
                .children(children)
                .build();
    }

    public CategorySummaryResponse toSummaryResponse(CategorySummaryView view) {
        return CategorySummaryResponse.builder()
                .publicId(view.getPublicId())
                .taxonomyId(view.getTaxonomyId())
                .name(view.getName())
                .slug(view.getSlug())
                .description(view.getDescription())
                .status(view.getStatus())
                .visibility(view.getVisibility())
                .sortOrder(view.getSortOrder())
                .mediaId(view.getMediaId())
                .isActive(view.getIsActive())
                .productCount(view.getProductCount())
                .activeProductCount(view.getActiveProductCount())
                .childCategoryCount(view.getChildCategoryCount())
                .createdAt(view.getCreatedAt())
                .updatedAt(view.getUpdatedAt())
                .build();
    }

    public CategoryStatusHistoryResponse toHistoryResponse(CategoryStatusHistory history) {
        return CategoryStatusHistoryResponse.builder()
                .publicId(history.getPublicId())
                .previousStatus(history.getPreviousStatus())
                .newStatus(history.getNewStatus())
                .reason(history.getReason())
                .changedBy(history.getChangedBy())
                .createdAt(history.getCreatedAt())
                .build();
    }

    public AttributeDefinitionResponse toAttributeDefinitionResponse(AttributeDefinition def,
                                                                      List<AttributeOptionResponse> options) {
        return AttributeDefinitionResponse.builder()
                .publicId(def.getPublicId())
                .name(def.getName())
                .code(def.getCode())
                .dataType(def.getDataType())
                .unit(def.getUnit())
                .required(def.getRequired())
                .filterable(def.getFilterable())
                .searchable(def.getSearchable())
                .variantDefining(def.getVariantDefining())
                .sortOrder(def.getSortOrder())
                .isActive(def.getIsActive())
                .options(options)
                .build();
    }

    public AttributeOptionResponse toAttributeOptionResponse(AttributeOption option) {
        return AttributeOptionResponse.builder()
                .publicId(option.getPublicId())
                .value(option.getValue())
                .label(option.getLabel())
                .sortOrder(option.getSortOrder())
                .isActive(option.getIsActive())
                .build();
    }

    public CategorySuggestionResponse toSuggestionResponse(CategorySuggestion suggestion) {
        return CategorySuggestionResponse.builder()
                .publicId(suggestion.getPublicId())
                .name(suggestion.getName())
                .description(suggestion.getDescription())
                .parentCategoryId(suggestion.getParentCategoryId())
                .reason(suggestion.getReason())
                .status(suggestion.getStatus())
                .requestedBy(suggestion.getRequestedBy())
                .reviewedBy(suggestion.getReviewedBy())
                .reviewedAt(suggestion.getReviewedAt())
                .createdAt(suggestion.getCreatedAt())
                .build();
    }
}

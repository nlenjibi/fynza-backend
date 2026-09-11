package ecommerce.modules.category.service.impl;

import ecommerce.modules.audit.constant.AuditAction;
import ecommerce.modules.audit.dto.AuditLogEntry;
import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.category.dto.request.CategorySearchRequest;
import ecommerce.modules.category.dto.request.CreateCategoryRequest;
import ecommerce.modules.category.dto.request.MoveCategoryRequest;
import ecommerce.modules.category.dto.request.UpdateCategoryRequest;
import ecommerce.modules.category.dto.response.*;
import ecommerce.modules.category.entity.Category;
import ecommerce.modules.category.entity.CategorySummaryView;
import ecommerce.modules.category.enums.CategoryStatus;
import ecommerce.modules.category.enums.CategoryVisibility;
import ecommerce.modules.category.exception.CategoryNotFoundException;
import ecommerce.modules.category.mapper.CategoryMapper;
import ecommerce.modules.category.repository.AttributeDefinitionRepository;
import ecommerce.modules.category.repository.AttributeOptionRepository;
import ecommerce.modules.category.repository.CategoryRepository;
import ecommerce.modules.category.repository.CategorySummaryViewRepository;
import ecommerce.modules.category.service.CategoryService;
import ecommerce.modules.category.spec.CategorySummarySpec;
import ecommerce.modules.category.validation.CategoryHierarchyValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository               categoryRepository;
    private final CategorySummaryViewRepository    summaryViewRepository;
    private final AttributeDefinitionRepository    attributeDefinitionRepository;
    private final AttributeOptionRepository        attributeOptionRepository;
    private final CategoryMapper                   mapper;
    private final CategoryHierarchyValidator       hierarchyValidator;
    private final AuditLogService                  auditLogService;

    @Override
    @Transactional
    public CategoryDetailResponse createCategory(CreateCategoryRequest request, UUID actorUserId) {
        String slug = resolveSlug(request.getSlug(), request.getName(), request.getTaxonomyId());

        Category.CategoryBuilder builder = Category.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .taxonomyId(request.getTaxonomyId())
                .visibility(request.getVisibility() != null ? request.getVisibility() : CategoryVisibility.PUBLIC)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .mediaId(request.getMediaId())
                .status(CategoryStatus.DRAFT)
                .isActive(true);

        if (request.getParentCategoryPublicId() != null) {
            Category parent = resolveCategory(request.getParentCategoryPublicId());
            hierarchyValidator.validateDepth(parent);
            builder.parentCategory(parent);
        }

        Category saved = categoryRepository.save(builder.build());

        auditLogService.log(AuditLogEntry.builder()
                .action(AuditAction.CATEGORY_CREATED)
                .entityType("CATEGORY")
                .entityPublicId(saved.getPublicId())
                .actorPublicId(actorUserId)
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());

        log.info("Category created: slug={}", saved.getSlug());
        return buildDetail(saved);
    }

    @Override
    @Transactional
    public CategoryDetailResponse updateCategory(UUID categoryPublicId, UpdateCategoryRequest request, UUID actorUserId) {
        Category category = resolveCategory(categoryPublicId);

        if (request.getName() != null && !request.getName().equals(category.getName())) {
            String newSlug = generateSlug(request.getName());
            if (!newSlug.equals(category.getSlug())
                    && slugExists(newSlug, category.getTaxonomyId())) {
                newSlug = generateUniqueSlug(newSlug, category.getTaxonomyId());
            }
            category.setName(request.getName());
            category.setSlug(newSlug);
        }
        if (request.getDescription() != null) category.setDescription(request.getDescription());
        if (request.getVisibility() != null)  category.setVisibility(request.getVisibility());
        if (request.getSortOrder() != null)   category.setSortOrder(request.getSortOrder());
        if (request.getMediaId() != null)     category.setMediaId(request.getMediaId());

        categoryRepository.save(category);

        auditLogService.log(AuditLogEntry.builder()
                .action(AuditAction.CATEGORY_UPDATED)
                .entityType("CATEGORY")
                .entityPublicId(category.getPublicId())
                .actorPublicId(actorUserId)
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());

        log.info("Category updated: publicId={}", categoryPublicId);
        return buildDetail(category);
    }

    @Override
    @Transactional
    public void moveCategory(UUID categoryPublicId, MoveCategoryRequest request, UUID actorUserId) {
        Category category = resolveCategory(categoryPublicId);
        Category newParent = request.getNewParentPublicId() != null
                ? resolveCategory(request.getNewParentPublicId()) : null;

        hierarchyValidator.validateMove(category, newParent);
        category.setParentCategory(newParent);
        categoryRepository.save(category);

        auditLogService.log(AuditLogEntry.builder()
                .action(AuditAction.CATEGORY_MOVED)
                .entityType("CATEGORY")
                .entityPublicId(category.getPublicId())
                .actorPublicId(actorUserId)
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());

        log.info("Category moved: publicId={}, newParent={}", categoryPublicId, request.getNewParentPublicId());
    }

    @Override
    @Transactional
    public void deleteCategory(UUID categoryPublicId, UUID actorUserId) {
        Category category = resolveCategory(categoryPublicId);
        category.setStatus(CategoryStatus.DELETED);
        category.setIsActive(false);
        categoryRepository.save(category);

        auditLogService.log(AuditLogEntry.builder()
                .action(AuditAction.CATEGORY_DELETED)
                .entityType("CATEGORY")
                .entityPublicId(category.getPublicId())
                .actorPublicId(actorUserId)
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());

        log.info("Category soft-deleted: publicId={}", categoryPublicId);
    }

    @Override
    public CategoryDetailResponse getCategoryByPublicId(UUID publicId) {
        Category category = resolveCategory(publicId);
        return buildDetail(category);
    }

    @Override
    public CategoryDetailResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new CategoryNotFoundException(slug));
        return buildDetail(category);
    }

    @Override
    public List<CategorySummaryResponse> getRootCategories(Long taxonomyId) {
        List<CategorySummaryView> views = taxonomyId != null
                ? summaryViewRepository.findByTaxonomyIdAndParentIdIsNullOrderBySortOrderAsc(taxonomyId)
                : summaryViewRepository.findByParentIdIsNullAndIsActiveTrueOrderBySortOrderAsc();
        return views.stream().map(mapper::toSummaryResponse).toList();
    }

    @Override
    public List<CategorySummaryResponse> getChildren(UUID parentPublicId) {
        Category parent = resolveCategory(parentPublicId);
        return summaryViewRepository.findByParentIdOrderBySortOrderAsc(parent.getId())
                .stream().map(mapper::toSummaryResponse).toList();
    }

    @Override
    public List<CategoryTreeResponse> getCategoryTree(Long taxonomyId) {
        List<Category> roots = taxonomyId != null
                ? categoryRepository.findByTaxonomyIdAndParentCategoryIsNullOrderBySortOrderAsc(taxonomyId)
                : categoryRepository.findByParentCategoryIsNullOrderBySortOrderAsc();
        return roots.stream().map(this::buildTree).toList();
    }

    @Override
    public Page<CategorySummaryResponse> searchCategories(CategorySearchRequest request, Pageable pageable) {
        return summaryViewRepository.findAll(CategorySummarySpec.from(request), pageable)
                .map(mapper::toSummaryResponse);
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private Category resolveCategory(UUID publicId) {
        return categoryRepository.findByPublicId(publicId)
                .orElseThrow(() -> new CategoryNotFoundException(publicId));
    }

    private CategoryTreeResponse buildTree(Category category) {
        List<Category> children = categoryRepository.findByParentCategory_IdOrderBySortOrderAsc(category.getId());
        List<CategoryTreeResponse> childResponses = children.stream().map(this::buildTree).toList();
        return mapper.toTreeResponse(category, childResponses);
    }

    private CategoryDetailResponse buildDetail(Category category) {
        List<AttributeDefinitionResponse> attributes = attributeDefinitionRepository
                .findByCategoryIdAndIsActiveTrueOrderBySortOrderAsc(category.getId())
                .stream()
                .map(def -> {
                    var options = attributeOptionRepository
                            .findByAttributeDefinitionIdAndIsActiveTrueOrderBySortOrderAsc(def.getId())
                            .stream().map(mapper::toAttributeOptionResponse).toList();
                    return mapper.toAttributeDefinitionResponse(def, options);
                })
                .toList();

        List<CategorySummaryResponse> children = summaryViewRepository
                .findByParentIdOrderBySortOrderAsc(category.getId())
                .stream().map(mapper::toSummaryResponse).toList();

        return mapper.toDetailResponse(category, attributes, children);
    }

    private String resolveSlug(String requested, String name, Long taxonomyId) {
        String base = (requested != null && !requested.isBlank()) ? requested : generateSlug(name);
        if (slugExists(base, taxonomyId)) {
            return generateUniqueSlug(base, taxonomyId);
        }
        return base;
    }

    private boolean slugExists(String slug, Long taxonomyId) {
        return taxonomyId != null
                ? categoryRepository.existsBySlugAndTaxonomyId(slug, taxonomyId)
                : categoryRepository.existsBySlug(slug);
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
    }

    private String generateUniqueSlug(String base, Long taxonomyId) {
        String candidate = base;
        int counter = 2;
        while (slugExists(candidate, taxonomyId)) {
            candidate = base + "-" + counter++;
        }
        return candidate;
    }
}

package ecommerce.modules.category.service.impl;

import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.category.dto.CategoryCreateRequest;
import ecommerce.modules.category.dto.CategoryResponse;
import ecommerce.modules.category.entity.Category;
import ecommerce.modules.category.repository.CategoryRepository;
import ecommerce.modules.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    private CategoryResponse toCategoryResponse(Category category) {
        LocalDateTime createdAt = category.getCreatedAt() != null
                ? LocalDateTime.ofInstant(category.getCreatedAt(), ZoneId.systemDefault()) : null;
        LocalDateTime updatedAt = category.getUpdatedAt() != null
                ? LocalDateTime.ofInstant(category.getUpdatedAt(), ZoneId.systemDefault()) : null;
        UUID parentId = category.getParentCategory() != null
                ? category.getParentCategory().getPublicId() : null;
        return CategoryResponse.builder()
                .id(category.getPublicId())
                .name(category.getName())
                .description(category.getDescription())
                .parentCategoryId(parentId)
                .featured(category.getFeatured())
                .image(category.getImage())
                .slug(category.getSlug())
                .isActive(category.getIsActive())
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    private void updateCategoryFields(Category category, CategoryCreateRequest request) {
        if (request.getName() != null) category.setName(request.getName());
        if (request.getDescription() != null) category.setDescription(request.getDescription());
        if (request.getImage() != null) category.setImage(request.getImage());
        if (request.getFeatured() != null) category.setFeatured(request.getFeatured());
        if (request.getIsActive() != null) category.setIsActive(request.getIsActive());
        if (request.getParentCategoryId() != null) {
            Category parent = categoryRepository.findByPublicId(request.getParentCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found: " + request.getParentCategoryId()));
            category.setParentCategory(parent);
        }
    }

    @Override
    @Cacheable(value = "categories", key = "'all'")
    public List<CategoryResponse> findAll() {
        log.debug("Fetching all categories");
        return categoryRepository.findAll().stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    @Override
    @Cacheable(value = "categories", key = "#id")
    public CategoryResponse findById(UUID id) {
        log.debug("Fetching category by ID: {}", id);
        Category category = categoryRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
        return toCategoryResponse(category);
    }

    @Override
    @Cacheable(value = "categories", key = "'tree'")
    public List<CategoryResponse> findTree() {
        log.debug("Fetching category tree");
        List<Category> rootCategories = categoryRepository.findRootCategories();
        return rootCategories.stream()
                .map(this::buildTree)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse create(CategoryCreateRequest request) {
        log.info("Creating new category: {}", request.getName());

        String slug = request.getSlug() != null ? request.getSlug() : generateSlug(request.getName());
        if (categoryRepository.existsBySlug(slug)) {
            slug = generateUniqueSlug(slug);
        }

        Category.CategoryBuilder builder = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .image(request.getImage())
                .slug(slug)
                .featured(request.getFeatured() != null ? request.getFeatured() : false)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true);

        if (request.getParentCategoryId() != null) {
            Category parent = categoryRepository.findByPublicId(request.getParentCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found: " + request.getParentCategoryId()));
            builder.parentCategory(parent);
        }

        Category savedCategory = categoryRepository.save(builder.build());
        log.info("Category created successfully with ID: {}", savedCategory.getPublicId());

        return toCategoryResponse(savedCategory);
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse update(UUID id, CategoryCreateRequest request) {
        log.info("Updating category with ID: {}", id);

        Category category = findCategoryById(id);

        if (request.getName() != null && !request.getName().equals(category.getName())) {
            String newSlug = generateSlug(request.getName());
            if (!newSlug.equals(category.getSlug()) && categoryRepository.existsBySlug(newSlug)) {
                newSlug = generateUniqueSlug(newSlug);
            }
            category.setSlug(newSlug);
        }

        updateCategoryFields(category, request);

        Category updatedCategory = categoryRepository.save(category);
        log.info("Category updated successfully: {}", updatedCategory.getPublicId());

        return toCategoryResponse(updatedCategory);
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void delete(UUID id) {
        log.info("Deleting category with ID: {}", id);
        Category category = findCategoryById(id);
        categoryRepository.delete(category);
        log.info("Category deleted successfully: {}", id);
    }

    private Category findCategoryById(UUID id) {
        return categoryRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
    }

    private String generateUniqueSlug(String baseSlug) {
        String uniqueSlug = baseSlug;
        int counter = 1;
        while (categoryRepository.existsBySlug(uniqueSlug)) {
            uniqueSlug = baseSlug + "-" + counter;
            counter++;
        }
        return uniqueSlug;
    }

    private CategoryResponse buildTree(Category category) {
        CategoryResponse response = toCategoryResponse(category);
        List<CategoryResponse> children = category.getSubcategories().stream()
                .map(this::buildTree)
                .toList();
        response.setChildren(children);
        return response;
    }

    // ==================== GraphQL Resolver Methods ====================

    @Override
    public CategoryResponse mapToResponse(Category category) {
        return toCategoryResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryResponse> getAllCategories(Pageable pageable) {
        return categoryRepository.findAll(pageable).map(this::toCategoryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(UUID id) {
        Category category = categoryRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
        return toCategoryResponse(category);
    }

    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        return create(request);
    }

    @Override
    @Cacheable(value = "categories", key = "'active'")
    public List<CategoryResponse> findActiveCategories() {
        log.debug("Fetching active categories");
        return categoryRepository.findByIsActive(true).stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse updateStatus(UUID id, Boolean isActive) {
        log.info("Updating category {} status to: {}", id, isActive);
        Category category = categoryRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
        category.setIsActive(isActive);
        Category saved = categoryRepository.save(category);
        log.info("Category {} status updated to: {}", id, isActive);
        return toCategoryResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getCategoryStats() {
        log.debug("Fetching category statistics");
        long total = categoryRepository.count();
        long active = categoryRepository.countActiveCategories();
        long subcategories = categoryRepository.countSubcategories();
        long parentCategories = categoryRepository.countParentCategories();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCategories", total);
        stats.put("activeCategories", active);
        stats.put("subcategories", subcategories);
        stats.put("parentCategories", parentCategories);

        return stats;
    }
}

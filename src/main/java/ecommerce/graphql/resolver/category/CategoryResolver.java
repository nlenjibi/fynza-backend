package ecommerce.graphql.resolver.category;

import ecommerce.common.response.PaginatedResponse;
import ecommerce.graphql.dto.CategoryPage;
import ecommerce.graphql.dto.CategoryStats;
import ecommerce.graphql.input.CategoryCreateInput;
import ecommerce.graphql.input.CategoryFilterInput;
import ecommerce.graphql.input.CategoryUpdateInput;
import ecommerce.graphql.input.PageInput;
import ecommerce.graphql.input.SortDirection;
import ecommerce.modules.category.dto.CategoryCreateRequest;
import ecommerce.modules.category.dto.CategoryResponse;
import ecommerce.modules.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CategoryResolver {

    private final CategoryService categoryService;

    // =========================================================================
    // PUBLIC QUERIES
    // =========================================================================

    @QueryMapping
    public CategoryResponse category(@Argument UUID id) {
        log.info("GQL category(id={})", id);
        return categoryService.findById(id);
    }

    @QueryMapping
    public CategoryPage categories(@Argument PageInput pagination) {
        log.info("GQL categories");
        Pageable pageable = toPageable(pagination);
        Page<CategoryResponse> page = categoryService.getAllCategories(pageable);
        return CategoryPage.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    @QueryMapping
    public List<CategoryResponse> activeCategories() {
        log.info("GQL activeCategories");
        return categoryService.findActiveCategories();
    }

    @QueryMapping
    public List<CategoryResponse> categoryTree() {
        log.info("GQL categoryTree");
        return categoryService.findTree();
    }

    @QueryMapping
    public List<CategoryResponse> featuredCategories() {
        log.info("GQL featuredCategories");
        return categoryService.findActiveCategories().stream()
                .filter(c -> Boolean.TRUE.equals(c.getFeatured()))
                .toList();
    }

    // =========================================================================
    // FILTERED QUERIES
    // =========================================================================

    @QueryMapping
    public CategoryPage filteredCategories(@Argument CategoryFilterInput filter,
                                            @Argument PageInput pagination) {
        log.info("GQL filteredCategories");
        Pageable pageable = toPageable(pagination);
        Page<CategoryResponse> page = categoryService.getAllCategories(pageable);

        List<CategoryResponse> filtered = page.getContent().stream()
                .filter(c -> {
                    if (filter == null) return true;
                    if (filter.getName() != null && !c.getName().toLowerCase().contains(filter.getName().toLowerCase()))
                        return false;
                    if (filter.getDescription() != null && !c.getDescription().toLowerCase().contains(filter.getDescription().toLowerCase()))
                        return false;
                    if (filter.getIsActive() != null && !filter.getIsActive().equals(c.getIsActive()))
                        return false;
                    if (filter.getFeatured() != null && !filter.getFeatured().equals(c.getFeatured()))
                        return false;
                    return true;
                })
                .toList();

        return CategoryPage.builder()
                .content(filtered)
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    @QueryMapping
    public CategoryPage rootCategories(@Argument Boolean featured,
                                         @Argument PageInput pagination) {
        log.info("GQL rootCategories(featured={})", featured);
        List<CategoryResponse> allActive = categoryService.findActiveCategories();
        List<CategoryResponse> rootCats = allActive.stream()
                .filter(c -> c.getParentCategoryId() == null)
                .filter(c -> featured == null || featured.equals(c.getFeatured()))
                .toList();

        return CategoryPage.builder()
                .content(rootCats)
                .pageInfo(PaginatedResponse.from(Page.empty()))
                .build();
    }

    // =========================================================================
    // ADMIN QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryStats categoryStats() {
        log.info("GQL categoryStats");
        Map<String, Object> stats = categoryService.getCategoryStats();
        return CategoryStats.builder()
                .totalCategories((Long) stats.getOrDefault("totalCategories", 0L))
                .activeCategories((Long) stats.getOrDefault("activeCategories", 0L))
                .featuredCategories((Long) stats.getOrDefault("featuredCategories", 0L))
                .rootCategories((Long) stats.getOrDefault("rootCategories", 0L))
                .build();
    }

    // =========================================================================
    // SELLER/ADMIN MUTATIONS
    // =========================================================================

    @MutationMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public CategoryResponse createCategory(@Argument CategoryCreateInput input) {
        log.info("GQL createCategory(name={})", input.getName());
        CategoryCreateRequest request = CategoryCreateRequest.builder()
                .name(input.getName())
                .description(input.getDescription())
                .parentCategoryId(input.getParentCategoryId())
                .image(input.getImage())
                .slug(input.getSlug())
                .featured(input.getFeatured())
                .isActive(input.getIsActive())
                .build();
        return categoryService.create(request);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public CategoryResponse updateCategory(@Argument UUID id,
                                            @Argument CategoryUpdateInput input) {
        log.info("GQL updateCategory(id={})", id);
        CategoryCreateRequest request = CategoryCreateRequest.builder()
                .name(input.getName())
                .description(input.getDescription())
                .parentCategoryId(input.getParentCategoryId())
                .image(input.getImage())
                .slug(input.getSlug())
                .featured(input.getFeatured())
                .isActive(input.getIsActive())
                .build();
        return categoryService.update(id, request);
    }

    // =========================================================================
    // ADMIN MUTATIONS
    // =========================================================================

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public boolean deleteCategory(@Argument UUID id) {
        log.info("GQL deleteCategory(id={})", id);
        categoryService.delete(id);
        return true;
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse updateCategoryStatus(@Argument UUID id,
                                                   @Argument boolean isActive) {
        log.info("GQL updateCategoryStatus(id={}, active={})", id, isActive);
        return categoryService.updateStatus(id, isActive);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private Pageable toPageable(PageInput input) {
        if (input == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "name"));
        }
        Sort sort = input.getDirection() == SortDirection.DESC
                ? Sort.by(input.getSortBy()).descending()
                : Sort.by(input.getSortBy()).ascending();
        return PageRequest.of(input.getPage(), input.getSize(), sort);
    }
}

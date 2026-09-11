package ecommerce.graphql.resolver.category;

import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.category.dto.request.CategorySearchRequest;
import ecommerce.modules.category.dto.response.*;
import ecommerce.modules.category.enums.CategoryStatus;
import ecommerce.modules.category.enums.CategorySuggestionStatus;
import ecommerce.modules.category.enums.CategoryVisibility;
import ecommerce.modules.category.service.CategoryAttributeService;
import ecommerce.modules.category.service.CategoryService;
import ecommerce.modules.category.service.CategoryStatusService;
import ecommerce.modules.category.service.CategorySuggestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CategoryGraphqlController {

    private final CategoryService          categoryService;
    private final CategoryStatusService    statusService;
    private final CategoryAttributeService attributeService;
    private final CategorySuggestionService suggestionService;

    // ── Public ────────────────────────────────────────────────────────────────

    @QueryMapping
    public CategoryDetailResponse category(@Argument String publicId) {
        log.debug("GQL category publicId={}", publicId);
        return categoryService.getCategoryByPublicId(UUID.fromString(publicId));
    }

    @QueryMapping
    public CategoryDetailResponse categoryBySlug(@Argument String slug) {
        log.debug("GQL categoryBySlug slug={}", slug);
        return categoryService.getCategoryBySlug(slug);
    }

    @QueryMapping
    public List<CategorySummaryResponse> rootCategories(@Argument Long taxonomyId) {
        log.debug("GQL rootCategories taxonomyId={}", taxonomyId);
        return categoryService.getRootCategories(taxonomyId);
    }

    @QueryMapping
    public List<CategorySummaryResponse> categoryChildren(@Argument String parentPublicId) {
        log.debug("GQL categoryChildren parentPublicId={}", parentPublicId);
        return categoryService.getChildren(UUID.fromString(parentPublicId));
    }

    @QueryMapping
    public List<CategoryTreeResponse> categoryTree(@Argument Long taxonomyId) {
        log.debug("GQL categoryTree taxonomyId={}", taxonomyId);
        return categoryService.getCategoryTree(taxonomyId);
    }

    @QueryMapping
    public Page<CategorySummaryResponse> categories(
            @Argument String query,
            @Argument String status,
            @Argument String visibility,
            @Argument Long taxonomyId,
            @Argument Boolean isActive,
            @Argument Integer page,
            @Argument Integer size) {

        CategorySearchRequest req = CategorySearchRequest.builder()
                .query(query)
                .status(status != null ? CategoryStatus.valueOf(status) : null)
                .visibility(visibility != null ? CategoryVisibility.valueOf(visibility) : null)
                .taxonomyId(taxonomyId)
                .isActive(isActive)
                .build();

        return categoryService.searchCategories(req,
                PageRequest.of(page != null ? page : 0, size != null ? size : 20));
    }

    @QueryMapping
    public List<AttributeDefinitionResponse> categoryAttributes(@Argument String categoryPublicId) {
        log.debug("GQL categoryAttributes categoryPublicId={}", categoryPublicId);
        return attributeService.getAttributesByCategory(UUID.fromString(categoryPublicId));
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @QueryMapping
    @PreAuthorize("hasAuthority('category.manage')")
    public List<CategoryStatusHistoryResponse> categoryStatusHistory(@Argument String categoryPublicId) {
        log.debug("GQL categoryStatusHistory categoryPublicId={}", categoryPublicId);
        return statusService.getStatusHistory(UUID.fromString(categoryPublicId));
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('category.manage')")
    public Page<CategorySuggestionResponse> categorySuggestions(
            @Argument String status,
            @Argument Integer page,
            @Argument Integer size) {
        CategorySuggestionStatus s = status != null ? CategorySuggestionStatus.valueOf(status) : null;
        return suggestionService.getSuggestions(s,
                PageRequest.of(page != null ? page : 0, size != null ? size : 20));
    }

    // ── Authenticated ─────────────────────────────────────────────────────────

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Page<CategorySuggestionResponse> myCategorySuggestions(
            @Argument Integer page,
            @Argument Integer size,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL myCategorySuggestions userId={}", principal.getId());
        return suggestionService.getMySuggestions(principal.getId(),
                PageRequest.of(page != null ? page : 0, size != null ? size : 20));
    }
}

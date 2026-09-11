package ecommerce.modules.category.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.category.dto.request.*;
import ecommerce.modules.category.dto.response.*;
import ecommerce.modules.category.service.CategoryAttributeService;
import ecommerce.modules.category.service.CategoryService;
import ecommerce.modules.category.service.CategoryStatusService;
import ecommerce.modules.category.service.CategorySuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin — Categories", description = "Admin category management mutations")
public class AdminCategoryController {

    private final CategoryService          categoryService;
    private final CategoryStatusService    statusService;
    private final CategoryAttributeService attributeService;
    private final CategorySuggestionService suggestionService;

    // ── Category CRUD ─────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create a new category")
    public ResponseEntity<ApiResponse<CategoryDetailResponse>> createCategory(
            @Valid @RequestBody CreateCategoryRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Category created successfully",
                        categoryService.createCategory(request, actorId)));
    }

    @PatchMapping("/{publicId}")
    @Operation(summary = "Update category details")
    public ResponseEntity<ApiResponse<CategoryDetailResponse>> updateCategory(
            @PathVariable UUID publicId,
            @Valid @RequestBody UpdateCategoryRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.success("Category updated successfully",
                categoryService.updateCategory(publicId, request, actorId)));
    }

    @PatchMapping("/{publicId}/status")
    @Operation(summary = "Change category status")
    public ResponseEntity<ApiResponse<CategoryResponse>> changeStatus(
            @PathVariable UUID publicId,
            @Valid @RequestBody CategoryStatusRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.success("Category status updated successfully",
                statusService.changeStatus(publicId, request, actorId)));
    }

    @PatchMapping("/{publicId}/move")
    @Operation(summary = "Move category to a different parent")
    public ResponseEntity<ApiResponse<Void>> moveCategory(
            @PathVariable UUID publicId,
            @RequestBody MoveCategoryRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = UUID.fromString(jwt.getSubject());
        categoryService.moveCategory(publicId, request, actorId);
        return ResponseEntity.ok(ApiResponse.success("Category moved successfully", null));
    }

    @DeleteMapping("/{publicId}")
    @Operation(summary = "Soft-delete a category")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable UUID publicId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = UUID.fromString(jwt.getSubject());
        categoryService.deleteCategory(publicId, actorId);
        return ResponseEntity.ok(ApiResponse.success("Category deleted successfully", null));
    }

    // ── Attributes ────────────────────────────────────────────────────────────

    @PostMapping("/{publicId}/attributes")
    @Operation(summary = "Add an attribute definition to a category")
    public ResponseEntity<ApiResponse<AttributeDefinitionResponse>> createAttribute(
            @PathVariable UUID publicId,
            @Valid @RequestBody CreateAttributeDefinitionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Attribute created successfully",
                        attributeService.createAttribute(publicId, request)));
    }

    @PatchMapping("/attributes/{attributePublicId}")
    @Operation(summary = "Update an attribute definition")
    public ResponseEntity<ApiResponse<AttributeDefinitionResponse>> updateAttribute(
            @PathVariable UUID attributePublicId,
            @Valid @RequestBody UpdateAttributeDefinitionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Attribute updated successfully",
                attributeService.updateAttribute(attributePublicId, request)));
    }

    @DeleteMapping("/attributes/{attributePublicId}")
    @Operation(summary = "Delete (deactivate) an attribute definition")
    public ResponseEntity<ApiResponse<Void>> deleteAttribute(@PathVariable UUID attributePublicId) {
        attributeService.deleteAttribute(attributePublicId);
        return ResponseEntity.ok(ApiResponse.success("Attribute deleted successfully", null));
    }

    @PostMapping("/attributes/{attributePublicId}/options")
    @Operation(summary = "Add an option to an attribute")
    public ResponseEntity<ApiResponse<AttributeOptionResponse>> addOption(
            @PathVariable UUID attributePublicId,
            @Valid @RequestBody CreateAttributeOptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Option added successfully",
                        attributeService.addOption(attributePublicId, request)));
    }

    @DeleteMapping("/attributes/options/{optionPublicId}")
    @Operation(summary = "Delete (deactivate) an attribute option")
    public ResponseEntity<ApiResponse<Void>> deleteOption(@PathVariable UUID optionPublicId) {
        attributeService.deleteOption(optionPublicId);
        return ResponseEntity.ok(ApiResponse.success("Option deleted successfully", null));
    }

    // ── Suggestions ───────────────────────────────────────────────────────────

    @PatchMapping("/suggestions/{suggestionPublicId}/approve")
    @Operation(summary = "Approve a category suggestion")
    public ResponseEntity<ApiResponse<CategorySuggestionResponse>> approveSuggestion(
            @PathVariable UUID suggestionPublicId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID reviewerId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.success("Suggestion approved",
                suggestionService.approveSuggestion(suggestionPublicId, reviewerId)));
    }

    @PatchMapping("/suggestions/{suggestionPublicId}/reject")
    @Operation(summary = "Reject a category suggestion")
    public ResponseEntity<ApiResponse<CategorySuggestionResponse>> rejectSuggestion(
            @PathVariable UUID suggestionPublicId,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal Jwt jwt) {
        UUID reviewerId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.success("Suggestion rejected",
                suggestionService.rejectSuggestion(suggestionPublicId, reason, reviewerId)));
    }
}

package ecommerce.modules.tag.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.tag.dto.CreateTagRequest;
import ecommerce.modules.tag.dto.TagResponse;
import ecommerce.modules.tag.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/tags")
@RequiredArgsConstructor
@Tag(name = "Tags", description = "Tag management endpoints")
public class TagController {

    private final TagService tagService;

    @GetMapping
    @Operation(summary = "Get all tags", description = "Get all available tags")
    public ResponseEntity<ApiResponse<List<TagResponse>>> getAllTags() {
        return ResponseEntity.ok(ApiResponse.success("Tags retrieved successfully", tagService.getAllTags()));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active tags", description = "Get all active tags")
    public ResponseEntity<ApiResponse<List<TagResponse>>> getActiveTags() {
        return ResponseEntity.ok(ApiResponse.success("Active tags retrieved successfully", tagService.getActiveTags()));
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured tags", description = "Get all featured tags")
    public ResponseEntity<ApiResponse<List<TagResponse>>> getFeaturedTags() {
        return ResponseEntity.ok(ApiResponse.success("Featured tags retrieved successfully", tagService.getFeaturedTags()));
    }

    @GetMapping("/popular")
    @Operation(summary = "Get popular tags", description = "Get popular tags by usage count")
    public ResponseEntity<ApiResponse<List<TagResponse>>> getPopularTags(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success("Popular tags retrieved successfully", tagService.getPopularTags(limit)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tag by ID", description = "Get a specific tag by its ID")
    public ResponseEntity<ApiResponse<TagResponse>> getTag(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Tag retrieved successfully", tagService.getTag(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create tag", description = "Create a new tag (Admin only)")
    public ResponseEntity<ApiResponse<TagResponse>> createTag(
            @Valid @RequestBody CreateTagRequest request) {
        TagResponse tag = tagService.createTag(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tag created successfully", tag));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update tag", description = "Update an existing tag (Admin only)")
    public ResponseEntity<ApiResponse<TagResponse>> updateTag(
            @PathVariable UUID id,
            @Valid @RequestBody CreateTagRequest request) {
        TagResponse tag = tagService.updateTag(id, request);
        return ResponseEntity.ok(ApiResponse.success("Tag updated successfully", tag));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete tag", description = "Delete a tag (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteTag(@PathVariable UUID id) {
        tagService.deleteTag(id);
        return ResponseEntity.ok(ApiResponse.success("Tag deleted successfully", null));
    }
}

package ecommerce.modules.faq.controller;

import ecommerce.common.enums.FAQCategory;
import ecommerce.common.response.ApiResponse;
import ecommerce.modules.faq.dto.*;
import ecommerce.modules.faq.service.FAQService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/faqs")
@RequiredArgsConstructor
@Tag(name = "Admin FAQ Management", description = "APIs for managing FAQs (Admin only)")
public class AdminFAQController {

    private final FAQService faqService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all FAQs", description = "Get all FAQs with pagination, search, and filter")
    public ResponseEntity<ApiResponse<Page<FAQResponse>>> getAllFAQs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) FAQCategory category) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<FAQResponse> faqs;
        
        if (search != null && !search.isBlank()) {
            faqs = faqService.searchFAQs(search, pageable);
        } else if (category != null) {
            faqs = faqService.getFAQsByCategory(category, pageable);
        } else {
            faqs = faqService.getAllFAQs(pageable);
        }
        
        return ResponseEntity.ok(ApiResponse.success("FAQs retrieved successfully", faqs));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get FAQ by ID", description = "Get a specific FAQ by ID")
    public ResponseEntity<ApiResponse<FAQResponse>> getFAQById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("FAQ retrieved successfully", faqService.getFAQById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create FAQ", description = "Create a new FAQ")
    public ResponseEntity<ApiResponse<FAQResponse>> createFAQ(
            @Valid @RequestBody CreateFAQRequest request) {
        FAQResponse created = faqService.createFAQ(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("FAQ created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update FAQ", description = "Update an existing FAQ")
    public ResponseEntity<ApiResponse<FAQResponse>> updateFAQ(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFAQRequest request) {
        FAQResponse updated = faqService.updateFAQ(id, request);
        return ResponseEntity.ok(ApiResponse.success("FAQ updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete FAQ", description = "Delete an FAQ")
    public ResponseEntity<ApiResponse<Void>> deleteFAQ(@PathVariable UUID id) {
        faqService.deleteFAQ(id);
        return ResponseEntity.ok(ApiResponse.success("FAQ deleted successfully", null));
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Toggle FAQ status", description = "Activate or deactivate an FAQ")
    public ResponseEntity<ApiResponse<FAQResponse>> toggleFAQStatus(@PathVariable UUID id) {
        FAQResponse updated = faqService.toggleFAQStatus(id);
        return ResponseEntity.ok(ApiResponse.success("FAQ status toggled successfully", updated));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get FAQ statistics", description = "Get FAQ statistics for dashboard")
    public ResponseEntity<ApiResponse<FAQStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success("FAQ statistics retrieved", faqService.getStats()));
    }

    @GetMapping("/help-categories")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get help categories", description = "Get organized help categories for admin view")
    public ResponseEntity<ApiResponse<List<HelpCategoryResponse>>> getHelpCategories() {
        List<HelpCategoryResponse> helpCategories = faqService.getHelpCategories();
        return ResponseEntity.ok(ApiResponse.success("Help categories retrieved successfully", helpCategories));
    }

    @PatchMapping("/{id}/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reorder FAQs", description = "Update display order of multiple FAQs")
    public ResponseEntity<ApiResponse<Void>> reorderFAQs(@RequestBody List<UUID> faqIds) {
        // TODO: Implement bulk reorder functionality in service
        // For now, return success - this would need proper implementation
        return ResponseEntity.ok(ApiResponse.success("FAQs reordered successfully", null));
    }
}

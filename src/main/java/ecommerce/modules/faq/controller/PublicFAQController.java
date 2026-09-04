package ecommerce.modules.faq.controller;

import ecommerce.common.enums.FAQCategory;
import ecommerce.common.response.ApiResponse;
import ecommerce.graphql.dto.FAQCategoryCount;
import ecommerce.modules.faq.dto.*;
import ecommerce.modules.faq.service.FAQService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/faqs")
@RequiredArgsConstructor
@Tag(name = "Public FAQ", description = "Public APIs for FAQ browsing")
public class PublicFAQController {

    private final FAQService faqService;

    @GetMapping
    @Operation(summary = "Get public FAQs", description = "Get active FAQs with pagination, search, and category filter")
    public ResponseEntity<ApiResponse<Page<FAQResponse>>> getPublicFAQs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "displayOrder") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) FAQCategory category) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<FAQResponse> faqs;
        
        if (search != null && !search.isBlank()) {
            faqs = faqService.searchFAQs(search, pageable);
        } else if (category != null) {
            faqs = faqService.getFAQsByCategory(category, pageable);
        } else {
            faqs = faqService.getActiveFAQs(pageable);
        }
        
        return ResponseEntity.ok(ApiResponse.success("FAQs retrieved successfully", faqs));
    }

    @GetMapping("/all")
    @Operation(summary = "Get all public FAQs", description = "Get all active FAQs without pagination")
    public ResponseEntity<ApiResponse<List<FAQResponse>>> getAllPublicFAQs() {
        List<FAQResponse> faqs = faqService.getPublicFAQs();
        return ResponseEntity.ok(ApiResponse.success("All FAQs retrieved successfully", faqs));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get FAQ by ID", description = "Get a specific FAQ by ID and increment view count")
    public ResponseEntity<ApiResponse<FAQResponse>> getFAQById(@PathVariable UUID id) {
        FAQResponse faq = faqService.getFAQById(id);
        // Increment view count
        faqService.incrementViewCount(id);
        return ResponseEntity.ok(ApiResponse.success("FAQ retrieved successfully", faq));
    }

    @GetMapping("/categories")
    @Operation(summary = "Get FAQ categories", description = "Get FAQ categories with counts")
    public ResponseEntity<ApiResponse<List<FAQCategoryCount>>> getFAQCategories() {
        List<FAQResponse> faqs = faqService.getPublicFAQs();
        
        List<FAQCategoryCount> categoryCounts = faqs.stream()
                .collect(java.util.stream.Collectors.groupingBy(FAQResponse::getCategory, java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .map(entry -> FAQCategoryCount.builder()
                        .category(entry.getKey())
                        .count(entry.getValue().intValue())
                        .build())
                .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success("FAQ categories retrieved successfully", categoryCounts));
    }

    @GetMapping("/help-categories")
    @Operation(summary = "Get help categories", description = "Get organized help categories with FAQs")
    public ResponseEntity<ApiResponse<List<HelpCategoryResponse>>> getHelpCategories() {
        List<HelpCategoryResponse> helpCategories = faqService.getHelpCategories();
        return ResponseEntity.ok(ApiResponse.success("Help categories retrieved successfully", helpCategories));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get FAQ statistics", description = "Get public FAQ statistics")
    public ResponseEntity<ApiResponse<FAQStatsResponse>> getFAQStats() {
        FAQStatsResponse stats = faqService.getStats();
        return ResponseEntity.ok(ApiResponse.success("FAQ statistics retrieved successfully", stats));
    }
}

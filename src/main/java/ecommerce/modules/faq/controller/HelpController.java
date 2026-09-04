package ecommerce.modules.faq.controller;

import ecommerce.common.enums.FAQCategory;
import ecommerce.common.response.ApiResponse;
import ecommerce.modules.contact.dto.ContactMessageRequest;
import ecommerce.modules.contact.service.ContactService;
import ecommerce.modules.faq.dto.*;
import ecommerce.modules.faq.service.FAQService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/help")
@RequiredArgsConstructor
@Tag(name = "Help & FAQ", description = "Public and customer help APIs")
public class HelpController {

    private final FAQService faqService;
    private final ContactService contactService;

    @GetMapping("/faqs")
    @Operation(summary = "Get public FAQs", description = "Get all active FAQs (public access)")
    public ResponseEntity<ApiResponse<Page<FAQResponse>>> getPublicFAQs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) FAQCategory category) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<FAQResponse> faqs;
        
        if (category != null) {
            faqs = faqService.getFAQsByCategory(category, pageable);
        } else {
            faqs = faqService.getActiveFAQs(pageable);
        }
        
        return ResponseEntity.ok(ApiResponse.success("FAQs retrieved successfully", faqs));
    }

    @GetMapping("/faqs/{id}")
    @Operation(summary = "Get FAQ by ID", description = "Get a specific FAQ by ID (public access)")
    public ResponseEntity<ApiResponse<FAQResponse>> getFAQById(@PathVariable UUID id) {
        FAQResponse response = faqService.incrementViewCount(id);
        return ResponseEntity.ok(ApiResponse.success("FAQ retrieved successfully", response));
    }

    @GetMapping("/help/categories")
    @Operation(summary = "Get help categories", description = "Get all help categories with FAQs")
    public ResponseEntity<ApiResponse<List<HelpCategoryResponse>>> getHelpCategories() {
        List<HelpCategoryResponse> categories = Arrays.stream(FAQCategory.values())
                .map(category -> {
                    Page<FAQResponse> faqs = faqService.getFAQsByCategory(
                            category, PageRequest.of(0, 10));
                    return HelpCategoryResponse.builder()
                            .name(category.name())
                            .slug(category.name().toLowerCase())
                            .faqs(faqs.getContent())
                            .build();
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success("Help categories retrieved", categories));
    }

    @GetMapping("/help/search")
    @Operation(summary = "Search FAQs", description = "Search FAQs by keyword (public access)")
    public ResponseEntity<ApiResponse<Page<FAQResponse>>> searchFAQs(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<FAQResponse> results = faqService.searchFAQs(query, pageable);
        
        return ResponseEntity.ok(ApiResponse.success("Search results retrieved", results));
    }

    @GetMapping("/help/contact")
    @Operation(summary = "Get contact options", description = "Get available contact options")
    public ResponseEntity<ApiResponse<ContactOptionsResponse>> getContactOptions() {
        ContactOptionsResponse response = ContactOptionsResponse.builder()
                .liveChat("https://fynza.com/chat")
                .emailSupport("support@fynza.com")
                .phoneSupport("030 274 0642")
                .phoneHours("Mon-Sat: 8am-8pm GMT")
                .build();
        
        return ResponseEntity.ok(ApiResponse.success("Contact options retrieved", response));
    }

    @PostMapping("/customer/help/contact")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Submit contact form", description = "Submit a contact form (authenticated users)")
    public ResponseEntity<ApiResponse<Void>> submitContactForm(
            @Valid @RequestBody ContactMessageRequest request) {
        contactService.submitMessage(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Contact message submitted successfully", null));
    }

    @PostMapping("/help/contact")
    @Operation(summary = "Submit contact form (guest)", description = "Submit a contact form (guest users)")
    public ResponseEntity<ApiResponse<Void>> submitContactFormGuest(
            @Valid @RequestBody ContactMessageRequest request) {
        contactService.submitMessage(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Contact message submitted successfully", null));
    }
}

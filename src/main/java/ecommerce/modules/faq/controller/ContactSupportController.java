package ecommerce.modules.faq.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.faq.dto.ContactOptionsResponse;
import ecommerce.modules.faq.service.FAQService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/contact")
@RequiredArgsConstructor
@Tag(name = "Contact Support", description = "Contact and support information")
public class ContactSupportController {

    private final FAQService faqService;

    @GetMapping("/options")
    @Operation(summary = "Get contact options", description = "Get available contact and support options")
    public ResponseEntity<ApiResponse<ContactOptionsResponse>> getContactOptions() {
        ContactOptionsResponse contactOptions = faqService.getContactOptions();
        return ResponseEntity.ok(ApiResponse.success("Contact options retrieved successfully", contactOptions));
    }
}

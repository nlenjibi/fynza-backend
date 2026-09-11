package ecommerce.modules.category.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.category.dto.request.CategorySuggestionRequest;
import ecommerce.modules.category.dto.response.CategorySuggestionResponse;
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
@RequestMapping("/v1/sellers/me/categories")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('category.suggestion.create') or hasRole('ADMIN')")
@Tag(name = "Seller — Categories", description = "Seller category suggestion mutations")
public class SellerCategoryController {

    private final CategorySuggestionService suggestionService;

    @PostMapping("/suggestions")
    @Operation(summary = "Submit a category suggestion")
    public ResponseEntity<ApiResponse<CategorySuggestionResponse>> submitSuggestion(
            @Valid @RequestBody CategorySuggestionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Suggestion submitted successfully",
                        suggestionService.submitSuggestion(request, userId)));
    }
}

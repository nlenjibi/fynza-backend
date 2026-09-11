package ecommerce.modules.category.service;

import ecommerce.modules.category.dto.request.CategorySuggestionRequest;
import ecommerce.modules.category.dto.response.CategorySuggestionResponse;
import ecommerce.modules.category.enums.CategorySuggestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CategorySuggestionService {

    CategorySuggestionResponse submitSuggestion(CategorySuggestionRequest request, UUID requestedBy);

    Page<CategorySuggestionResponse> getSuggestions(CategorySuggestionStatus status, Pageable pageable);

    Page<CategorySuggestionResponse> getMySuggestions(UUID requestedBy, Pageable pageable);

    CategorySuggestionResponse approveSuggestion(UUID suggestionPublicId, UUID reviewedBy);

    CategorySuggestionResponse rejectSuggestion(UUID suggestionPublicId, String reason, UUID reviewedBy);
}

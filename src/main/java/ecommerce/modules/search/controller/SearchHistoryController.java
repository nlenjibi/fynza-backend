package ecommerce.modules.search.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.search.service.SearchHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/search/history")
@RequiredArgsConstructor
@Tag(name = "Search History", description = "Search history management")
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

    @Operation(summary = "Clear own search history")
    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> clearHistory(
            @AuthenticationPrincipal UserPrincipal principal) {
        searchHistoryService.clearHistory(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Search history cleared.", null));
    }
}

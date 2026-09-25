package ecommerce.modules.search.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.search.service.SearchIndexService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin/search")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Search", description = "Search index administration")
public class AdminSearchController {

    private final SearchIndexService searchIndexService;

    @Operation(summary = "Trigger full product search index rebuild")
    @PostMapping("/reindex/products")
    public ResponseEntity<ApiResponse<String>> reindex() {
        searchIndexService.triggerReindex();
        return ResponseEntity.accepted()
                .body(ApiResponse.success("Search reindex triggered. Running in background.", "TRIGGERED"));
    }

    @Operation(summary = "Retry failed search index documents")
    @PostMapping("/retry")
    public ResponseEntity<ApiResponse<String>> retry() {
        searchIndexService.retryFailed();
        return ResponseEntity.ok(ApiResponse.success("Search index retry completed.", "OK"));
    }
}

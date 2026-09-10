package ecommerce.modules.store.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.store.dto.request.StoreSearchRequest;
import ecommerce.modules.store.dto.response.StoreSummaryResponse;
import ecommerce.modules.store.service.StoreManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/stores")
@RequiredArgsConstructor
@Tag(name = "Store — Public", description = "Public store browsing")
public class PublicStoreController {

    private final StoreManagementService storeManagementService;

    @GetMapping
    @Operation(summary = "Browse active stores")
    public ResponseEntity<ApiResponse<Page<StoreSummaryResponse>>> searchStores(
            StoreSearchRequest params,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Stores retrieved",
                storeManagementService.searchStores(params, pageable)));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get store by slug")
    public ResponseEntity<ApiResponse<StoreSummaryResponse>> getStoreBySlug(
            @PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success("Store retrieved",
                storeManagementService.getStoreBySlug(slug)));
    }
}

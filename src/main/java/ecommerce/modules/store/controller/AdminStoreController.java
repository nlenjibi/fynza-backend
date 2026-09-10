package ecommerce.modules.store.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.store.dto.request.StoreSearchRequest;
import ecommerce.modules.store.dto.request.StoreStatusRequest;
import ecommerce.modules.store.dto.response.StoreDetailResponse;
import ecommerce.modules.store.dto.response.StoreResponse;
import ecommerce.modules.store.dto.response.StoreStatusHistoryResponse;
import ecommerce.modules.store.dto.response.StoreSummaryResponse;
import ecommerce.modules.store.service.StoreManagementService;
import ecommerce.modules.store.service.StoreStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/stores")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Store — Admin", description = "Admin store management")
public class AdminStoreController {

    private final StoreManagementService storeManagementService;
    private final StoreStatusService     storeStatusService;

    @GetMapping
    @Operation(summary = "Search all stores")
    public ResponseEntity<ApiResponse<Page<StoreSummaryResponse>>> searchStores(
            StoreSearchRequest params,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Stores retrieved",
                storeManagementService.searchStores(params, pageable)));
    }

    @GetMapping("/{storeId}")
    @PreAuthorize("hasAuthority('store.read')")
    @Operation(summary = "Get store details by public ID")
    public ResponseEntity<ApiResponse<StoreDetailResponse>> getStore(@PathVariable UUID storeId) {
        return ResponseEntity.ok(ApiResponse.success("Store retrieved",
                storeManagementService.getStoreByPublicId(storeId)));
    }

    @PatchMapping("/{storeId}/status")
    @PreAuthorize("hasAuthority('store.suspend') or hasAuthority('store.activate') or hasAuthority('store.close')")
    @Operation(summary = "Change store status")
    public ResponseEntity<ApiResponse<StoreResponse>> changeStatus(
            @PathVariable UUID storeId,
            @Valid @RequestBody StoreStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Store status updated",
                storeStatusService.changeStatus(storeId, principal.getId(), request)));
    }

    @GetMapping("/{storeId}/status-history")
    @PreAuthorize("hasAuthority('store.read')")
    @Operation(summary = "Get store status history")
    public ResponseEntity<ApiResponse<List<StoreStatusHistoryResponse>>> getStatusHistory(
            @PathVariable UUID storeId) {
        return ResponseEntity.ok(ApiResponse.success("Status history retrieved",
                storeManagementService.getStatusHistory(storeId)));
    }
}

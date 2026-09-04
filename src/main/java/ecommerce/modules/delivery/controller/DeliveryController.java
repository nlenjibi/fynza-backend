package ecommerce.modules.delivery.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.response.PaginatedResponse;
import ecommerce.modules.delivery.dto.DeliveryFeeRequest;
import ecommerce.modules.delivery.dto.DeliveryFeeResponse;
import ecommerce.modules.delivery.dto.DeliveryRegionRequest;
import ecommerce.modules.delivery.dto.DeliveryRegionResponse;
import ecommerce.modules.delivery.service.DeliveryService;
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
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Delivery Management", description = "APIs for managing delivery regions and fees")
public class DeliveryController {

    private final DeliveryService deliveryService;

    // ==================== Region Endpoints ====================

    @PostMapping("/admin/delivery/regions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create region", description = "Create a new delivery region - ADMIN only")
    public ResponseEntity<ApiResponse<DeliveryRegionResponse>> createRegion(
            @Valid @RequestBody DeliveryRegionRequest request) {
        DeliveryRegionResponse response = deliveryService.createRegion(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Region created successfully", response));
    }

    @GetMapping("/admin/delivery/regions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all regions", description = "Get all delivery regions - ADMIN only")
    public ResponseEntity<ApiResponse<PaginatedResponse<DeliveryRegionResponse>>> getAllRegions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<DeliveryRegionResponse> regions = deliveryService.getAllRegions(pageable);
        return ResponseEntity.ok(ApiResponse.success("Regions retrieved successfully", PaginatedResponse.from(regions)));
    }

    @GetMapping("/admin/delivery/regions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get region by ID", description = "Get delivery region by ID - ADMIN only")
    public ResponseEntity<ApiResponse<DeliveryRegionResponse>> getRegionById(@PathVariable UUID id) {
        DeliveryRegionResponse region = deliveryService.getRegionById(id);
        return ResponseEntity.ok(ApiResponse.success("Region retrieved successfully", region));
    }

    @GetMapping("/delivery/regions")
    @Operation(summary = "Get active regions", description = "Get all active delivery regions - PUBLIC")
    public ResponseEntity<ApiResponse<List<DeliveryRegionResponse>>> getActiveRegions() {
        List<DeliveryRegionResponse> regions = deliveryService.getActiveRegions();
        return ResponseEntity.ok(ApiResponse.success("Regions retrieved successfully", regions));
    }

    @PutMapping("/admin/delivery/regions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update region", description = "Update delivery region - ADMIN only")
    public ResponseEntity<ApiResponse<DeliveryRegionResponse>> updateRegion(
            @PathVariable UUID id,
            @Valid @RequestBody DeliveryRegionRequest request) {
        DeliveryRegionResponse response = deliveryService.updateRegion(id, request);
        return ResponseEntity.ok(ApiResponse.success("Region updated successfully", response));
    }

    @DeleteMapping("/admin/delivery/regions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete region", description = "Soft delete delivery region - ADMIN only")
    public ResponseEntity<ApiResponse<Void>> deleteRegion(@PathVariable UUID id) {
        deliveryService.deleteRegion(id);
        return ResponseEntity.ok(ApiResponse.success("Region deleted successfully", null));
    }

    // ==================== Delivery Fee Endpoints ====================

    @PostMapping("/admin/delivery/fees")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create delivery fee", description = "Create a new delivery fee - ADMIN only")
    public ResponseEntity<ApiResponse<DeliveryFeeResponse>> createDeliveryFee(
            @Valid @RequestBody DeliveryFeeRequest request) {
        DeliveryFeeResponse response = deliveryService.createDeliveryFee(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Delivery fee created successfully", response));
    }

    @GetMapping("/admin/delivery/fees")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all delivery fees", description = "Get all delivery fees - ADMIN only")
    public ResponseEntity<ApiResponse<PaginatedResponse<DeliveryFeeResponse>>> getAllDeliveryFees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<DeliveryFeeResponse> fees = deliveryService.getAllDeliveryFees(pageable);
        return ResponseEntity.ok(ApiResponse.success("Delivery fees retrieved successfully", PaginatedResponse.from(fees)));
    }

    @GetMapping("/admin/delivery/fees/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get delivery fee by ID", description = "Get delivery fee by ID - ADMIN only")
    public ResponseEntity<ApiResponse<DeliveryFeeResponse>> getDeliveryFeeById(@PathVariable UUID id) {
        DeliveryFeeResponse fee = deliveryService.getDeliveryFeeById(id);
        return ResponseEntity.ok(ApiResponse.success("Delivery fee retrieved successfully", fee));
    }

    @GetMapping("/delivery/fees")
    @Operation(summary = "Get active delivery fees", description = "Get all active delivery fees - PUBLIC")
    public ResponseEntity<ApiResponse<List<DeliveryFeeResponse>>> getActiveDeliveryFees() {
        List<DeliveryFeeResponse> fees = deliveryService.getActiveDeliveryFees();
        return ResponseEntity.ok(ApiResponse.success("Delivery fees retrieved successfully", fees));
    }

    @GetMapping("/delivery/fees/region/{regionId}")
    @Operation(summary = "Get delivery fees by region", description = "Get delivery fees for a specific region - PUBLIC")
    public ResponseEntity<ApiResponse<List<DeliveryFeeResponse>>> getDeliveryFeesByRegion(@PathVariable UUID regionId) {
        List<DeliveryFeeResponse> fees = deliveryService.getDeliveryFeesByRegion(regionId);
        return ResponseEntity.ok(ApiResponse.success("Delivery fees retrieved successfully", fees));
    }

    @PutMapping("/admin/delivery/fees/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update delivery fee", description = "Update delivery fee - ADMIN only")
    public ResponseEntity<ApiResponse<DeliveryFeeResponse>> updateDeliveryFee(
            @PathVariable UUID id,
            @Valid @RequestBody DeliveryFeeRequest request) {
        DeliveryFeeResponse response = deliveryService.updateDeliveryFee(id, request);
        return ResponseEntity.ok(ApiResponse.success("Delivery fee updated successfully", response));
    }

    @DeleteMapping("/admin/delivery/fees/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete delivery fee", description = "Soft delete delivery fee - ADMIN only")
    public ResponseEntity<ApiResponse<Void>> deleteDeliveryFee(@PathVariable UUID id) {
        deliveryService.deleteDeliveryFee(id);
        return ResponseEntity.ok(ApiResponse.success("Delivery fee deleted successfully", null));
    }
}

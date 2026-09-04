package ecommerce.modules.admin.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.response.PaginatedResponse;
import ecommerce.common.enums.ProductStatus;
import ecommerce.modules.admin.dto.AdminAnalyticsDto;
import ecommerce.modules.admin.service.AdminService;
import ecommerce.modules.order.dto.OrderResponse;
import ecommerce.modules.order.dto.OrderStatusUpdateRequest;
import ecommerce.modules.order.service.OrderService;
import ecommerce.modules.product.dto.AdminProductStatsResponse;
import ecommerce.modules.product.dto.ProductResponse;
import ecommerce.modules.product.service.ProductService;
import ecommerce.modules.user.dto.UserDto;
import ecommerce.modules.user.dto.BulkUserUpdateRequest;
import ecommerce.modules.user.service.UserService;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Management", description = "Admin management endpoints")
public class AdminController {

    private final AdminService adminService;
    private final OrderService orderService;
    private final ProductService productService;
    private final UserService userService;

    @GetMapping("/orders")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all orders", description = "Get all orders for admin")
    public ResponseEntity<ApiResponse<PaginatedResponse<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<OrderResponse> orders = orderService.getAllOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully",PaginatedResponse.from(orders)));

            }

    @GetMapping("/orders/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search orders", description = "Search orders with filters for admin")
    public ResponseEntity<ApiResponse<PaginatedResponse<OrderResponse>>> searchOrders(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) java.time.LocalDateTime dateFrom,
            @RequestParam(required = false) java.time.LocalDateTime dateTo,
            @RequestParam(required = false) java.math.BigDecimal minAmount,
            @RequestParam(required = false) java.math.BigDecimal maxAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        var criteria = OrderService.OrderSearchCriteria.builder()
                .query(query)
                .status(status)
                .paymentStatus(paymentStatus)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .build();
        
        Page<OrderResponse> orders = orderService.searchOrdersAdmin(criteria, pageable);
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", PaginatedResponse.from(orders)));
    }

    @GetMapping("/orders/export")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Export orders to CSV", description = "Export orders as CSV file for admin")
    public ResponseEntity<byte[]> exportOrders(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) java.time.LocalDateTime dateFrom,
            @RequestParam(required = false) java.time.LocalDateTime dateTo,
            @RequestParam(required = false) java.math.BigDecimal minAmount,
            @RequestParam(required = false) java.math.BigDecimal maxAmount) {
        
        var criteria = OrderService.OrderSearchCriteria.builder()
                .query(query)
                .status(status)
                .paymentStatus(paymentStatus)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .build();
        
        String csv = orderService.exportOrdersToCSV(criteria);
        
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=orders.csv")
                .body(csv.getBytes());
    }

    @PutMapping("/orders/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update order status", description = "Update order status as admin")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable UUID id,
            @RequestBody OrderStatusUpdateRequest request) {
        var updateRequest = new ecommerce.modules.order.dto.OrderUpdateRequest();
        updateRequest.setStatus(request.getStatus());
        updateRequest.setAdminNotes(request.getNotes());
        
        OrderResponse order = orderService.updateOrderStatus(id, updateRequest.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully", order));
    }

    @PutMapping("/products/{id}/inventory")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update product inventory", description = "Update product inventory as admin")
    public ResponseEntity<ApiResponse<ProductResponse>> updateInventory(
            @PathVariable UUID id,
            @RequestParam Integer stock) {
        ProductResponse product = productService.findById(id);
        
        var updateRequest = new ecommerce.modules.product.dto.UpdateProductRequest();
        updateRequest.setStock(stock);
        
        ProductResponse updated = productService.update(id, updateRequest);
        return ResponseEntity.ok(ApiResponse.success("Inventory updated successfully", updated));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users", description = "Get paginated list of users")
    public ResponseEntity<ApiResponse<Page<UserDto>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<UserDto> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user by ID", description = "Get a specific user by ID")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable UUID id) {
        UserDto user = userService.getUserById(id)
                .orElseThrow(() -> new ecommerce.common.exception.ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create user", description = "Create a new user")
    public ResponseEntity<ApiResponse<UserDto>> createUser(
            @Valid @RequestBody ecommerce.modules.user.dto.UserCreateRequest request) {
        UserDto user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created successfully", user));
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user", description = "Update an existing user")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody ecommerce.modules.user.dto.UserUpdateRequest request) {
        UserDto user = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", user));
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user", description = "Soft delete a user")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }

    @PatchMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user role", description = "Update user's role")
    public ResponseEntity<ApiResponse<UserDto>> updateUserRole(
            @PathVariable UUID id,
            @RequestParam String role) {
        UserDto user = userService.updateUserRole(id, 
                new ecommerce.modules.user.dto.UpdateUserRoleRequest(role));
        return ResponseEntity.ok(ApiResponse.success("User role updated successfully", user));
    }

    @PatchMapping("/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user status", description = "Lock/unlock/activate user account")
    public ResponseEntity<ApiResponse<UserDto>> updateUserStatus(
            @PathVariable UUID id,
            @RequestParam Boolean isActive) {
        UserDto user = userService.updateUserStatus(id, 
                new ecommerce.modules.user.dto.UserStatusRequest(isActive));
        return ResponseEntity.ok(ApiResponse.success("User status updated successfully", user));
    }

    @PostMapping("/users/bulk-update")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bulk update users", description = "Update multiple users at once (activate, deactivate, role change)")
    public ResponseEntity<ApiResponse<List<UserDto>>> bulkUpdateUsers(
            @Valid @RequestBody BulkUserUpdateRequest request) {
        List<UserDto> updatedUsers = userService.bulkUpdateUsers(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Bulk update completed successfully. Updated " + updatedUsers.size() + " users.", 
                updatedUsers));
    }

    @GetMapping("/customers/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get customer statistics", description = "Get customer statistics for admin")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCustomerStats() {
        Map<String, Object> stats = userService.getCustomerStats();
        return ResponseEntity.ok(ApiResponse.success("Customer statistics retrieved successfully", stats));
    }

    @GetMapping("/customers/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search customers", description = "Search customers by name, email, or phone")
    public ResponseEntity<ApiResponse<Page<UserDto>>> searchCustomers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<UserDto> customers = userService.searchCustomers(query, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Customers retrieved successfully", customers));
    }

    @GetMapping("/customers/export")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Export customers to CSV", description = "Export customers as CSV file")
    public ResponseEntity<byte[]> exportCustomers(
            @RequestParam(required = false) String status) {
        String csv = userService.exportCustomersToCSV(status);
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=customers.csv")
                .body(csv.getBytes());
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get admin analytics", description = "Get comprehensive analytics for admin dashboard with filter support")
    public ResponseEntity<ApiResponse<AdminAnalyticsDto>> getAnalytics(
            @RequestParam(defaultValue = "month") String filter) {
        AdminAnalyticsDto analytics = adminService.getAnalytics(filter);
        return ResponseEntity.ok(ApiResponse.success("Analytics retrieved successfully", analytics));
    }

    @GetMapping("/products")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all products", description = "Get all products for admin with filters")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<ProductResponse> products = productService.findBySellerId(null, status, null, search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", products));
    }

    @GetMapping("/products/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get product stats", description = "Get product statistics for admin")
    public ResponseEntity<ApiResponse<AdminProductStatsResponse>> getProductStats() {
        AdminProductStatsResponse stats = productService.getAdminProductStats();
        return ResponseEntity.ok(ApiResponse.success("Product stats retrieved successfully", stats));
    }

    @PatchMapping("/products/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve product", description = "Approve a pending product")
    public ResponseEntity<ApiResponse<ProductResponse>> approveProduct(@PathVariable UUID id) {
        ProductResponse product = productService.approveProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product approved successfully", product));
    }

    @PatchMapping("/products/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject product", description = "Reject a pending product")
    public ResponseEntity<ApiResponse<ProductResponse>> rejectProduct(
            @PathVariable UUID id,
            @RequestParam String reason) {
        ProductResponse product = productService.rejectProduct(id, reason);
        return ResponseEntity.ok(ApiResponse.success("Product rejected successfully", product));
    }

    @GetMapping("/customers/{customerId}/orders")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get customer orders", description = "Get all orders for a specific customer")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getCustomerOrders(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<OrderResponse> orders = orderService.getUserOrders(customerId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Customer orders retrieved successfully", orders));
    }

    // ==================== Seller Management ====================

    @GetMapping("/sellers")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all sellers", description = "Get all sellers with search and filters - ADMIN only")
    public ResponseEntity<ApiResponse<Page<UserDto>>> getAllSellers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<UserDto> sellers = userService.searchSellers(query, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Sellers retrieved successfully", sellers));
    }

    @GetMapping("/sellers/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get seller statistics", description = "Get seller statistics for admin")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSellerStats() {
        Map<String, Object> stats = userService.getSellerStats();
        return ResponseEntity.ok(ApiResponse.success("Seller statistics retrieved successfully", stats));
    }

    @PatchMapping("/sellers/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve seller", description = "Approve a pending seller")
    public ResponseEntity<ApiResponse<UserDto>> approveSeller(@PathVariable UUID id) {
        UserDto seller = userService.approveSeller(id);
        return ResponseEntity.ok(ApiResponse.success("Seller approved successfully", seller));
    }

    @PatchMapping("/sellers/{id}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Suspend seller", description = "Suspend an active seller")
    public ResponseEntity<ApiResponse<UserDto>> suspendSeller(@PathVariable UUID id) {
        UserDto seller = userService.suspendSeller(id);
        return ResponseEntity.ok(ApiResponse.success("Seller suspended successfully", seller));
    }

    @PatchMapping("/sellers/{id}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reactivate seller", description = "Reactivate a suspended seller")
    public ResponseEntity<ApiResponse<UserDto>> reactivateSeller(@PathVariable UUID id) {
        UserDto seller = userService.reactivateSeller(id);
        return ResponseEntity.ok(ApiResponse.success("Seller reactivated successfully", seller));
    }
}


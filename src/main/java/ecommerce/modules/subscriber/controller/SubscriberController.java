package ecommerce.modules.subscriber.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.response.PaginatedResponse;
import ecommerce.modules.subscriber.dto.SubscriberRequest;
import ecommerce.modules.subscriber.dto.SubscriberResponse;
import ecommerce.modules.subscriber.entity.Subscriber;
import ecommerce.modules.subscriber.service.SubscriberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Subscriber Management", description = "APIs for managing newsletter subscribers")
public class SubscriberController {

    private final SubscriberService subscriberService;

    @PostMapping("/subscribers")
    @Operation(summary = "Subscribe to newsletter", description = "Subscribe to newsletter - public endpoint")
    public ResponseEntity<ApiResponse<SubscriberResponse>> subscribe(
            @Valid @RequestBody SubscriberRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        SubscriberResponse response = subscriberService.subscribe(request, ipAddress);
        return ResponseEntity.ok(ApiResponse.success("Successfully subscribed to newsletter!", response));
    }

    @GetMapping("/subscribers/unsubscribe/{id}")
    @Operation(summary = "Unsubscribe from newsletter", description = "Unsubscribe using subscriber ID - public endpoint")
    public ResponseEntity<ApiResponse<SubscriberResponse>> unsubscribe(@PathVariable UUID id) {
        SubscriberResponse response = subscriberService.unsubscribe(id);
        return ResponseEntity.ok(ApiResponse.success("Successfully unsubscribed from newsletter", response));
    }

    @GetMapping("/admin/subscribers")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all subscribers", description = "Retrieve all subscribers with pagination - ADMIN only")
    public ResponseEntity<ApiResponse<PaginatedResponse<SubscriberResponse>>> getAllSubscribers(
            @RequestParam(required = false) Subscriber.SubscriberStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<SubscriberResponse> subscribers = subscriberService.getAllSubscribers(status, search, pageable);

        return ResponseEntity.ok(ApiResponse.success("Subscribers retrieved successfully", PaginatedResponse.from(subscribers)));
    }

    @GetMapping("/admin/subscribers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get subscriber by ID", description = "Retrieve subscriber details - ADMIN only")
    public ResponseEntity<ApiResponse<SubscriberResponse>> getSubscriberById(@PathVariable UUID id) {
        SubscriberResponse subscriber = subscriberService.getSubscriberById(id);
        return ResponseEntity.ok(ApiResponse.success("Subscriber retrieved successfully", subscriber));
    }

    @DeleteMapping("/admin/subscribers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete subscriber", description = "Soft delete subscriber - ADMIN only")
    public ResponseEntity<ApiResponse<Void>> deleteSubscriber(@PathVariable UUID id) {
        subscriberService.deleteSubscriber(id);
        return ResponseEntity.ok(ApiResponse.success("Subscriber deleted successfully", null));
    }

    @GetMapping("/admin/subscribers/export")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Export subscribers to CSV", description = "Export subscribers as CSV file - ADMIN only")
    public ResponseEntity<byte[]> exportSubscribers(
            @RequestParam(required = false) Subscriber.SubscriberStatus status) {

        String csv = subscriberService.exportToCSV(status);

        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=subscribers.csv")
                .body(csv.getBytes());
    }

    @GetMapping("/admin/subscribers/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get subscriber statistics", description = "Get subscriber statistics - ADMIN only")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getSubscriberStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", subscriberService.countTotalSubscribers());
        stats.put("active", subscriberService.countActiveSubscribers());
        stats.put("unsubscribed", subscriberService.countUnsubscribedSubscribers());

        return ResponseEntity.ok(ApiResponse.success("Statistics retrieved successfully", stats));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

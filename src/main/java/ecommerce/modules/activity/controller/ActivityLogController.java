package ecommerce.modules.activity.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.response.PaginatedResponse;
import ecommerce.modules.activity.entity.ActivityLog;
import ecommerce.modules.activity.service.ActivityLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/activity-logs")
@RequiredArgsConstructor
@Tag(name = "Activity Logs", description = "Activity log management endpoints")
@PreAuthorize("hasRole('ADMIN')")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    @GetMapping
    @Operation(summary = "Get all activity logs", description = "Get paginated list of all activity logs")
    public ResponseEntity<ApiResponse<PaginatedResponse<ActivityLog>>> getAllActivities(Pageable pageable) {
        Page<ActivityLog> activities = activityLogService.getAllActivities(pageable);
        return ResponseEntity.ok(ApiResponse.success("Activity logs retrieved successfully", PaginatedResponse.from(activities)));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user activity logs", description = "Get activity logs for a specific user")
    public ResponseEntity<ApiResponse<PaginatedResponse<ActivityLog>>> getUserActivities(
            @PathVariable UUID userId, Pageable pageable) {
        Page<ActivityLog> activities = activityLogService.getActivitiesByUser(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success("User activity logs retrieved successfully", PaginatedResponse.from(activities)));
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get activities by type", description = "Get activity logs filtered by type")
    public ResponseEntity<ApiResponse<PaginatedResponse<ActivityLog>>> getActivitiesByType(
            @PathVariable ActivityLog.ActivityType type, Pageable pageable) {
        Page<ActivityLog> activities = activityLogService.getActivitiesByType(type, pageable);
        return ResponseEntity.ok(ApiResponse.success("Activity logs retrieved successfully", PaginatedResponse.from(activities)));
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get activities by date range", description = "Get activity logs within a date range")
    public ResponseEntity<ApiResponse<PaginatedResponse<ActivityLog>>> getActivitiesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            Pageable pageable) {
        Page<ActivityLog> activities = activityLogService.getActivitiesByDateRange(start, end, pageable);
        return ResponseEntity.ok(ApiResponse.success("Activity logs retrieved successfully", PaginatedResponse.from(activities)));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "Get entity history", description = "Get activity history for a specific entity")
    public ResponseEntity<ApiResponse<List<ActivityLog>>> getEntityHistory(
            @PathVariable String entityType,
            @PathVariable UUID entityId) {
        List<ActivityLog> history = activityLogService.getEntityHistory(entityType, entityId);
        return ResponseEntity.ok(ApiResponse.success("Entity history retrieved successfully", history));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get activity summary", description = "Get summary of activities grouped by type")
    public ResponseEntity<ApiResponse<List<Object[]>>> getActivitySummary(
            @RequestParam(defaultValue = "30") int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Object[]> summary = activityLogService.getActivitySummary(since);
        return ResponseEntity.ok(ApiResponse.success("Activity summary retrieved successfully", summary));
    }
}

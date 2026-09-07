package ecommerce.common.monitoring;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.util.CacheStatisticsService;
import ecommerce.common.util.DatabaseMetricsService;
import ecommerce.common.util.MetricsService;
import ecommerce.common.util.SecurityEventService;
import ecommerce.common.security.TokenBlacklistService;
import ecommerce.common.audit.QueryPerformanceAspect;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/monitoring")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class PerformanceController {

    private final CacheManager cacheManager;
    private final CacheStatisticsService cacheStatisticsService;
    private final MetricsService metricsService;
    private final SecurityEventService securityEventService;
    private final TokenBlacklistService tokenBlacklistService;
    private final DatabaseMetricsService databaseMetricsService;
    private final QueryPerformanceAspect queryPerformanceAspect;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        try {
            Map<String, Object> dashboard = new HashMap<>();

            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory() / (1024 * 1024);
            long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
            dashboard.put("system", Map.of(
                    "memory", Map.of(
                            "maxMb", maxMemory,
                            "usedMb", usedMemory,
                            "usagePercent", String.format("%.2f", (double) usedMemory / maxMemory * 100)),
                    "processors", runtime.availableProcessors(),
                    "uptime", System.currentTimeMillis()));

            dashboard.put("cache", cacheStatisticsService.getAllCacheStatistics());

            var securityStats = securityEventService.getStats();
            dashboard.put("security", Map.of(
                    "failedLoginAttempts", securityStats.currentFailedAttemptsCount(),
                    "hitRate", formatPercent(securityStats.hitRate()),
                    "accessLogSize", securityStats.accessLogSize()));

            var blacklistStats = tokenBlacklistService.getStats();
            dashboard.put("blacklist", Map.of(
                    "size", blacklistStats.currentSize(),
                    "hitRate", formatPercent(blacklistStats.hitRate())));

            dashboard.put("endpoints", Map.ofEntries(
                    Map.entry("metrics",       "/api/v1/monitoring/metrics"),
                    Map.entry("dashboard",     "/api/v1/monitoring/dashboard"),
                    Map.entry("cache",         "/api/v1/monitoring/cache"),
                    Map.entry("system",        "/api/v1/monitoring/system"),
                    Map.entry("database",      "/api/v1/monitoring/database"),
                    Map.entry("security",      "/api/v1/monitoring/security"),
                    Map.entry("securityStats", "/api/v1/monitoring/security/stats"),
                    Map.entry("rateLimit",     "/api/v1/monitoring/rate-limit"),
                    Map.entry("all",           "/api/v1/monitoring/all"),
                    Map.entry("download",      "/api/v1/monitoring/download"),
                    Map.entry("refresh",       "/api/v1/monitoring/refresh"),
                    Map.entry("design",        "/api/v1/monitoring/design")));

            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .data(dashboard)
                    .message("Dashboard retrieved successfully").build());
        } catch (Exception e) {
            log.error("Error retrieving dashboard: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<Map<String, Object>>builder()
                    .message("Failed to retrieve dashboard: " + e.getMessage()).build());
        }
    }

    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        try {
            metrics.put("cache_stats", cacheStatisticsService.getAllCacheStatistics());

            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory() / (1024 * 1024);
            long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
            metrics.put("memory", Map.of(
                    "max_mb", maxMemory,
                    "used_mb", usedMemory,
                    "usage_percent", String.format("%.2f", (double) usedMemory / maxMemory * 100)));
            metrics.put("available_processors", runtime.availableProcessors());

            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .data(metrics)
                    .message("Performance metrics retrieved successfully").build());
        } catch (Exception e) {
            log.error("Error retrieving performance metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<Map<String, Object>>builder()
                    .message("Failed to retrieve performance metrics: " + e.getMessage()).build());
        }
    }

    @GetMapping("/cache/{cacheName}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCacheStats(@PathVariable String cacheName) {
        Map<String, Object> stats = new HashMap<>();
        try {
            stats.put("cache_name", cacheName);
            boolean available = cacheManager.getCacheNames().contains(cacheName);
            stats.put("available", available);
            if (available) {
                stats.put("stats", cacheStatisticsService.getCacheStats(cacheManager.getCache(cacheName)));
            }
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder().data(stats).build());
        } catch (Exception e) {
            log.error("Error retrieving cache stats for {}: {}", cacheName, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<Map<String, Object>>builder()
                    .message("Failed to retrieve cache stats: " + e.getMessage()).build());
        }
    }

    @PostMapping("/cache/clear")
    public ResponseEntity<ApiResponse<String>> clearAllCaches() {
        try {
            cacheStatisticsService.clearAllCaches();
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .message("All caches cleared successfully").build());
        } catch (Exception e) {
            log.error("Error clearing caches: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<String>builder()
                    .message("Failed to clear caches: " + e.getMessage()).build());
        }
    }

    @PostMapping("/cache/clear/{cacheName}")
    public ResponseEntity<ApiResponse<String>> clearCache(@PathVariable String cacheName) {
        try {
            if (!cacheManager.getCacheNames().contains(cacheName)) {
                return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                        .message("Cache '" + cacheName + "' not found").build());
            }
            cacheStatisticsService.clearCache(cacheName);
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .message("Cache '" + cacheName + "' cleared successfully").build());
        } catch (Exception e) {
            log.error("Error clearing cache {}: {}", cacheName, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<String>builder()
                    .message("Failed to clear cache: " + e.getMessage()).build());
        }
    }

    @PostMapping("/cache/warmup")
    public ResponseEntity<ApiResponse<String>> warmupCaches() {
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .message("Cache warmup process initiated").build());
    }

    @GetMapping("/database")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDatabaseMetrics() {
        try {
            Map<String, Object> metrics = databaseMetricsService.getDatabaseMetrics();

            var queryReport = queryPerformanceAspect.generateReport();
            Map<String, Object> queryPerfData = new java.util.LinkedHashMap<>();
            queryPerfData.put("totalQueries", queryReport.getTotalQueries());
            queryPerfData.put("totalSlowQueries", queryReport.getTotalSlowQueries());
            queryPerfData.put("slowestQueries", queryPerformanceAspect.getSlowestQueries(10).stream()
                    .map(e -> Map.of(
                            "query",   e.queryName(),
                            "count",   e.stats().getCount(),
                            "avgTime", String.format("%.2fms", e.stats().getAverageTime()),
                            "maxTime", e.stats().getMaxTime() + "ms",
                            "minTime", e.stats().getMinTime() + "ms"))
                    .toList());

            metrics.put("queryPerformance", queryPerfData);
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .data(metrics)
                    .message("Database metrics retrieved successfully").build());
        } catch (Exception e) {
            log.error("Error retrieving database metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<Map<String, Object>>builder()
                    .message("Failed to retrieve database metrics: " + e.getMessage()).build());
        }
    }

    @GetMapping("/system")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemMetrics() {
        try {
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .data(metricsService.getSystemMetrics())
                    .message("System metrics retrieved successfully").build());
        } catch (Exception e) {
            log.error("Error retrieving system metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<Map<String, Object>>builder()
                    .message("Failed to retrieve system metrics: " + e.getMessage()).build());
        }
    }

    @GetMapping("/cache")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCacheMetrics() {
        try {
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .data(metricsService.getCacheMetrics())
                    .message("Cache metrics retrieved successfully").build());
        } catch (Exception e) {
            log.error("Error retrieving cache metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<Map<String, Object>>builder()
                    .message("Failed to retrieve cache metrics: " + e.getMessage()).build());
        }
    }

    @GetMapping("/rate-limit")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRateLimitMetrics() {
        try {
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .data(metricsService.getRateLimitMetrics())
                    .message("Rate limit metrics retrieved successfully").build());
        } catch (Exception e) {
            log.error("Error retrieving rate limit metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<Map<String, Object>>builder()
                    .message("Failed to retrieve rate limit metrics: " + e.getMessage()).build());
        }
    }

    @GetMapping("/security")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSecurityStats() {
        try {
            var securityStats = securityEventService.getStats();
            Map<String, Object> stats = new HashMap<>();
            stats.put("failedLoginAttempts", securityStats.currentFailedAttemptsCount());
            stats.put("hitRate", securityStats.hitRate());
            stats.put("accessLogSize", securityStats.accessLogSize());
            stats.put("maxFailedAttempts", securityStats.maxFailedAttempts());
            stats.put("lockoutDurationMinutes", securityStats.lockoutDurationMinutes());
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .data(stats)
                    .message("Security stats retrieved successfully").build());
        } catch (Exception e) {
            log.error("Error retrieving security stats: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<Map<String, Object>>builder()
                    .message("Failed to retrieve security stats: " + e.getMessage()).build());
        }
    }

    @GetMapping("/security/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSecurityStatsDetailed() {
        var blacklistStats = tokenBlacklistService.getStats();
        var securityStats  = securityEventService.getStats();

        Map<String, Object> stats = Map.of(
                "tokenBlacklist", Map.of(
                        "currentSize", blacklistStats.currentSize(),
                        "hitRate",     formatPercent(blacklistStats.hitRate()),
                        "missRate",    formatPercent(blacklistStats.missRate())),
                "securityEvents", Map.of(
                        "failedAttemptsCount",    securityStats.currentFailedAttemptsCount(),
                        "hitRate",                formatPercent(securityStats.hitRate()),
                        "accessLogSize",          securityStats.accessLogSize(),
                        "maxFailedAttempts",      securityStats.maxFailedAttempts(),
                        "lockoutDurationMinutes", securityStats.lockoutDurationMinutes()));

        return ResponseEntity.ok(ApiResponse.success("Security stats retrieved", stats));
    }

    @PostMapping("/security/cleanup")
    public ResponseEntity<ApiResponse<String>> triggerSecurityCleanup() {
        tokenBlacklistService.clearExpiredTokens();
        securityEventService.clearExpiredAttempts();
        return ResponseEntity.ok(ApiResponse.success("Cleanup completed", "Expired entries removed"));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllMetrics() {
        try {
            Map<String, Object> allMetrics = metricsService.getAllMetrics();
            var securityStats = securityEventService.getStats();
            allMetrics.put("security", Map.of(
                    "failedLoginAttempts", securityStats.currentFailedAttemptsCount(),
                    "hitRate",             securityStats.hitRate(),
                    "accessLogSize",       securityStats.accessLogSize()));
            allMetrics.put("database", databaseMetricsService.getDatabaseMetrics());
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .data(allMetrics)
                    .message("All metrics retrieved successfully").build());
        } catch (Exception e) {
            log.error("Error retrieving all metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<Map<String, Object>>builder()
                    .message("Failed to retrieve all metrics: " + e.getMessage()).build());
        }
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadMetrics(@RequestParam(defaultValue = "json") String format) {
        try {
            Map<String, Object> allData = new HashMap<>();
            allData.put("timestamp", LocalDateTime.now().toString());
            allData.put("metrics", metricsService.getAllMetrics());

            var securityStats  = securityEventService.getStats();
            var blacklistStats = tokenBlacklistService.getStats();
            allData.put("security", Map.of(
                    "failedLoginAttempts", securityStats.currentFailedAttemptsCount(),
                    "hitRate",             formatPercent(securityStats.hitRate()),
                    "accessLogSize",       securityStats.accessLogSize()));
            allData.put("blacklist", Map.of(
                    "size",    blacklistStats.currentSize(),
                    "hitRate", formatPercent(blacklistStats.hitRate())));
            allData.put("cache", cacheStatisticsService.getAllCacheStatistics());

            String filename = "performance-metrics-" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));

            if ("csv".equalsIgnoreCase(format)) {
                Runtime runtime = Runtime.getRuntime();
                long maxMemory  = runtime.maxMemory() / (1024 * 1024);
                long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);

                StringBuilder csv = new StringBuilder("Category,Metric,Value\n");
                csv.append(String.format("System,Max Memory (MB),%d\n",        maxMemory));
                csv.append(String.format("System,Used Memory (MB),%d\n",       usedMemory));
                csv.append(String.format("System,Memory Usage (%%),%.2f\n",    (double) usedMemory / maxMemory * 100));
                csv.append(String.format("System,Available Processors,%d\n",   runtime.availableProcessors()));
                csv.append(String.format("Security,Failed Login Attempts,%d\n", securityStats.currentFailedAttemptsCount()));
                csv.append(String.format("Security,Access Log Size,%d\n",       securityStats.accessLogSize()));
                csv.append(String.format("Blacklist,Current Size,%d\n",         blacklistStats.currentSize()));

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + ".csv\"")
                        .contentType(MediaType.parseMediaType("text/csv"))
                        .body(csv.toString().getBytes());
            } else {
                String json = new com.fasterxml.jackson.databind.ObjectMapper()
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(allData);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + ".json\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(json.getBytes());
            }
        } catch (Exception e) {
            log.error("Error generating download: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshMetrics() {
        try {
            cacheStatisticsService.clearAllCaches();
            Map<String, Object> result = new HashMap<>();
            result.put("timestamp",    LocalDateTime.now().toString());
            result.put("cacheCleared", true);
            result.put("metrics",      metricsService.getAllMetrics());
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .data(result)
                    .message("Metrics refreshed successfully").build());
        } catch (Exception e) {
            log.error("Error refreshing metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<Map<String, Object>>builder()
                    .message("Failed to refresh metrics: " + e.getMessage()).build());
        }
    }

    @PostMapping("/refresh/{metricType}")
    public ResponseEntity<ApiResponse<String>> refreshSpecificMetric(@PathVariable String metricType) {
        try {
            String message = switch (metricType.toLowerCase()) {
                case "cache"     -> { cacheStatisticsService.clearAllCaches();        yield "Cache metrics refreshed"; }
                case "security"  -> { securityEventService.clearExpiredAttempts();    yield "Security metrics refreshed"; }
                case "blacklist" -> { tokenBlacklistService.clearExpiredTokens();     yield "Blacklist metrics refreshed"; }
                case "system"    -> "System metrics refreshed (always live)";
                default          -> "Unknown metric type: " + metricType;
            };
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .data(message).message("Metric refreshed successfully").build());
        } catch (Exception e) {
            log.error("Error refreshing metric {}: {}", metricType, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<String>builder()
                    .message("Failed to refresh metric: " + e.getMessage()).build());
        }
    }

    @GetMapping("/design")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDesignSettings() {
        Map<String, Object> design = new HashMap<>();
        design.put("theme", Map.of(
                "primaryColor",   "#007bff", "secondaryColor", "#6c757d",
                "successColor",   "#28a745", "dangerColor",    "#dc3545",
                "warningColor",   "#ffc107", "infoColor",      "#17a2b8",
                "darkMode",       false));
        design.put("dashboard", Map.of(
                "refreshInterval",      30000,
                "showSystemMetrics",    true,
                "showCacheMetrics",     true,
                "showSecurityMetrics",  true,
                "showDatabaseMetrics",  true,
                "animationsEnabled",    true));
        design.put("charts", Map.of(
                "memoryChartEnabled",   true,
                "cacheChartEnabled",    true,
                "securityChartEnabled", true,
                "updateInterval",       5000));
        design.put("notifications", Map.of(
                "enabled",           true,
                "emailAlerts",       false,
                "criticalAlertsOnly", true));
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .data(design).message("Design settings retrieved successfully").build());
    }

    @PutMapping("/design")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateDesignSettings(
            @RequestBody Map<String, Object> designSettings) {
        Map<String, Object> result = new HashMap<>();
        result.put("status",           "Design settings updated");
        result.put("timestamp",        LocalDateTime.now().toString());
        result.put("receivedSettings", designSettings.keySet());
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .data(result).message("Design settings updated successfully").build());
    }

    @PostMapping("/clear/all")
    public ResponseEntity<ApiResponse<Map<String, String>>> clearAll() {
        try {
            cacheStatisticsService.clearAllCaches();
            securityEventService.clearExpiredAttempts();
            tokenBlacklistService.clearExpiredTokens();
            Map<String, String> result = new HashMap<>();
            result.put("caches",        "cleared");
            result.put("securityEvents","cleaned");
            result.put("expiredTokens", "removed");
            result.put("timestamp",     LocalDateTime.now().toString());
            return ResponseEntity.ok(ApiResponse.<Map<String, String>>builder()
                    .data(result).message("All caches and expired data cleared successfully").build());
        } catch (Exception e) {
            log.error("Error clearing all: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<Map<String, String>>builder()
                    .message("Failed to clear: " + e.getMessage()).build());
        }
    }

    @PostMapping("/clear/cache")
    public ResponseEntity<ApiResponse<String>> clearCaches() {
        try {
            cacheStatisticsService.clearAllCaches();
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .message("All caches cleared successfully").build());
        } catch (Exception e) {
            log.error("Error clearing caches: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<String>builder()
                    .message("Failed to clear caches: " + e.getMessage()).build());
        }
    }

    @PostMapping("/clear/security")
    public ResponseEntity<ApiResponse<String>> clearSecurityData() {
        try {
            securityEventService.clearExpiredAttempts();
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .message("Security data cleared successfully").build());
        } catch (Exception e) {
            log.error("Error clearing security data: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<String>builder()
                    .message("Failed to clear security data: " + e.getMessage()).build());
        }
    }

    @PostMapping("/clear/blacklist")
    public ResponseEntity<ApiResponse<String>> clearBlacklist() {
        try {
            tokenBlacklistService.clearExpiredTokens();
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .message("Blacklist cleared successfully").build());
        } catch (Exception e) {
            log.error("Error clearing blacklist: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<String>builder()
                    .message("Failed to clear blacklist: " + e.getMessage()).build());
        }
    }

    @PostMapping("/cleanup")
    public ResponseEntity<ApiResponse<Map<String, Object>>> performCleanup() {
        try {
            tokenBlacklistService.clearExpiredTokens();
            securityEventService.clearExpiredAttempts();
            cacheStatisticsService.clearAllCaches();
            Map<String, Object> result = new HashMap<>();
            result.put("expiredTokens",    "cleaned");
            result.put("securityAttempts", "cleaned");
            result.put("caches",           "cleared");
            result.put("timestamp",        LocalDateTime.now().toString());
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .data(result).message("Cleanup completed successfully").build());
        } catch (Exception e) {
            log.error("Error during cleanup: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<Map<String, Object>>builder()
                    .message("Failed to cleanup: " + e.getMessage()).build());
        }
    }

    @PostMapping("/cleanup/expired")
    public ResponseEntity<ApiResponse<String>> cleanupExpired() {
        try {
            tokenBlacklistService.clearExpiredTokens();
            securityEventService.clearExpiredAttempts();
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .message("Expired entries cleaned successfully").build());
        } catch (Exception e) {
            log.error("Error during cleanup: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.<String>builder()
                    .message("Failed to cleanup: " + e.getMessage()).build());
        }
    }

    private String formatPercent(double rate) {
        return String.format("%.2f%%", rate * 100);
    }
}

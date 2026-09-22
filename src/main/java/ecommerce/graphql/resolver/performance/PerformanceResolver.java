package ecommerce.graphql.resolver.performance;

import ecommerce.graphql.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PerformanceResolver {

    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public SystemMetrics systemMetrics() {
        MemoryUsage heap    = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();
        long maxMb  = heap.getMax()  / (1024 * 1024);
        long usedMb = heap.getUsed() / (1024 * 1024);
        long nonHeapMb = nonHeap.getUsed() / (1024 * 1024);
        boolean warning = maxMb > 0 && (double) usedMb / maxMb > 0.85;

        return SystemMetrics.builder()
                .memory(MemoryInfo.builder()
                        .maxMb(maxMb)
                        .usedMb(usedMb)
                        .usagePercent(maxMb > 0 ? String.format("%.1f%%", (double) usedMb / maxMb * 100) : "N/A")
                        .warning(warning)
                        .build())
                .cpu(CpuInfo.builder()
                        .threadCount(threadBean.getThreadCount())
                        .peakThreads(threadBean.getPeakThreadCount())
                        .daemonThreads(threadBean.getDaemonThreadCount())
                        .nonHeapMemoryMb(nonHeapMb)
                        .build())
                .server(ServerInfo.builder()
                        .status("UP")
                        .message("Server is running normally")
                        .uptime(ManagementFactory.getRuntimeMXBean().getUptime())
                        .build())
                .rateLimits(List.of())
                .build();
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CacheMetrics cacheMetrics() {
        return CacheMetrics.builder()
                .actions(CacheActions.builder()
                        .canWarmup(true)
                        .canClearAll(true)
                        .build())
                .caches(List.of())
                .build();
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public DatabaseMetrics databaseMetrics() {
        return DatabaseMetrics.builder()
                .info(DatabaseInfo.builder()
                        .product("PostgreSQL")
                        .driver("PostgreSQL JDBC Driver")
                        .build())
                .connectionPool(ConnectionPoolInfo.builder()
                        .active(0)
                        .idle(0)
                        .total(0)
                        .max(0)
                        .utilization("N/A")
                        .health("UNKNOWN")
                        .build())
                .queryPerformance(QueryPerformanceInfo.builder()
                        .totalQueries(0L)
                        .slowQueries(0)
                        .avgTime("N/A")
                        .status("N/A")
                        .build())
                .build();
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public SecurityMetrics securityMetrics() {
        return SecurityMetrics.builder()
                .stats(SecurityStats.builder()
                        .failedLoginAttempts(0)
                        .hitRate("N/A")
                        .accessLogSize(0L)
                        .lockoutDurationMinutes(0)
                        .build())
                .actions(SecurityActions.builder()
                        .canCleanup(true)
                        .build())
                .build();
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PerformanceDashboard performanceDashboard() {
        return PerformanceDashboard.builder()
                .timestamp(java.time.Instant.now().toString())
                .system(systemMetrics())
                .cache(cacheMetrics())
                .database(databaseMetrics())
                .security(securityMetrics())
                .build();
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String exportPerformanceMetrics(@Argument String format) {
        log.info("GraphQL Query: exportPerformanceMetrics(format: {})", format);
        return "{}";
    }
}

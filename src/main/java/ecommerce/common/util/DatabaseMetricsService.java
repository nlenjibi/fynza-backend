package ecommerce.common.util;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * Service for collecting real database performance metrics from HikariCP connection pool.
 */
@Slf4j
@Service
public class DatabaseMetricsService {

    @Autowired(required = false)
    private DataSource dataSource;

    public Map<String, Object> getDatabaseMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("database", getDatabaseInfo());

        if (dataSource instanceof HikariDataSource hikariDS) {
            metrics.put("connectionPool", getConnectionPoolMetrics(hikariDS));
        } else {
            metrics.put("connectionPool", getDefaultPoolMetrics());
        }

        return metrics;
    }

    private Map<String, Object> getDatabaseInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        if (dataSource == null) {
            info.put("status", "DataSource not available");
            return info;
        }
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            info.put("databaseProductName", metaData.getDatabaseProductName());
            info.put("databaseProductVersion", metaData.getDatabaseProductVersion());
            info.put("driverName", metaData.getDriverName());
            info.put("driverVersion", metaData.getDriverVersion());
        } catch (SQLException e) {
            log.warn("Could not get database metadata: {}", e.getMessage());
            info.put("error", e.getMessage());
        }
        return info;
    }

    private Map<String, Object> getConnectionPoolMetrics(HikariDataSource hikariDS) {
        Map<String, Object> poolMetrics = new LinkedHashMap<>();
        try {
            HikariPoolMXBean poolMXBean = hikariDS.getHikariPoolMXBean();

            poolMetrics.put("poolName", hikariDS.getPoolName());
            poolMetrics.put("maximumPoolSize", hikariDS.getMaximumPoolSize());
            poolMetrics.put("minimumIdle", hikariDS.getMinimumIdle());
            poolMetrics.put("activeConnections", poolMXBean.getActiveConnections());
            poolMetrics.put("idleConnections", poolMXBean.getIdleConnections());
            poolMetrics.put("totalConnections", poolMXBean.getTotalConnections());
            poolMetrics.put("threadsAwaitingConnection", poolMXBean.getThreadsAwaitingConnection());

            int maxPool = hikariDS.getMaximumPoolSize();
            double utilization = maxPool > 0
                    ? ((double) poolMXBean.getActiveConnections() / maxPool) * 100 : 0;
            poolMetrics.put("connectionUtilization", String.format("%.1f%%", utilization));

            Map<String, String> health = new LinkedHashMap<>();
            if (poolMXBean.getActiveConnections() >= maxPool) {
                health.put("status", "CRITICAL");
                health.put("message", "Pool at maximum capacity!");
            } else if (poolMXBean.getActiveConnections() >= maxPool * 0.8) {
                health.put("status", "WARNING");
                health.put("message", "Pool approaching capacity");
            } else {
                health.put("status", "HEALTHY");
                health.put("message", "Pool operating normally");
            }
            poolMetrics.put("health", health);

        } catch (Exception e) {
            log.warn("Could not get HikariCP metrics: {}", e.getMessage());
            poolMetrics.put("error", e.getMessage());
            poolMetrics.putAll(getDefaultPoolMetrics());
        }
        return poolMetrics;
    }

    private Map<String, Object> getDefaultPoolMetrics() {
        Map<String, Object> defaultMetrics = new LinkedHashMap<>();
        defaultMetrics.put("status", "N/A");
        defaultMetrics.put("message", "HikariCP not configured");
        defaultMetrics.put("activeConnections", 0);
        defaultMetrics.put("idleConnections", 0);
        defaultMetrics.put("totalConnections", 0);
        defaultMetrics.put("maximumPoolSize", "Not configured");
        return defaultMetrics;
    }
}

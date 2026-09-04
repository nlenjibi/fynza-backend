package ecommerce.graphql.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PerformanceDashboard {
    private String timestamp;
    private SystemMetrics system;
    private CacheMetrics cache;
    private DatabaseMetrics database;
    private SecurityMetrics security;
}

package ecommerce.graphql.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceMetrics {
    private Double avgResponseTimeMs;
    private Double cacheHitRate;
    private Integer activeConnections;
    private Long uptimeSeconds;
}

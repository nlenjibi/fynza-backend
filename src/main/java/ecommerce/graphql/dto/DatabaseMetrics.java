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
public class DatabaseMetrics {
    private DatabaseInfo info;
    private ConnectionPoolInfo connectionPool;
    private QueryPerformanceInfo queryPerformance;
}

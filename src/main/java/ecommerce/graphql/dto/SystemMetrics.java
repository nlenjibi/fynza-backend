package ecommerce.graphql.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class SystemMetrics {
    private MemoryInfo memory;
    private CpuInfo cpu;
    private ServerInfo server;
    private List<Object> rateLimits;
}

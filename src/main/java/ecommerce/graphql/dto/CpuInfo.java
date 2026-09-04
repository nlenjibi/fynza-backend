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
public class CpuInfo {
    private int threadCount;
    private int peakThreads;
    private int daemonThreads;
    private long nonHeapMemoryMb;
}

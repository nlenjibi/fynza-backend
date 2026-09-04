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
public class CacheStatistics {
    private String cacheName;
    private long hits;
    private long misses;
    private String hitRate;
    private long size;
    private String hitRateStatus;
}

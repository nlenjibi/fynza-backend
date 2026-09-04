package ecommerce.modules.follow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowStatsResponse {
    private long totalFollowers;
    private long activeThisMonth;
    private long newThisWeek;
    private double avgFollowAge;
}

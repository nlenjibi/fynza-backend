package ecommerce.graphql.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowStats {
    private int totalFollowers;
    private int activeThisMonth;
    private int newThisWeek;
    private float avgFollowAge;
}

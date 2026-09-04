package ecommerce.graphql.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerStats {
    private Long totalSellers;
    private Long activeSellers;
    private Long pendingSellers;
    private Long suspendedSellers;
}

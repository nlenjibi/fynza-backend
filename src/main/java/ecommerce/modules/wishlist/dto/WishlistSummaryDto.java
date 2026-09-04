package ecommerce.modules.wishlist.dto;


import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
public class WishlistSummaryDto {

    private UUID userId;
    private Integer totalItems;
    private Integer inStockItems;
    private Integer outOfStockItems;
    private Integer itemsWithPriceDrops;
    private Integer purchasedItems;

    private BigDecimal totalValue;
    private BigDecimal totalSavings;

    private List<WishlistItemDto> items;
}

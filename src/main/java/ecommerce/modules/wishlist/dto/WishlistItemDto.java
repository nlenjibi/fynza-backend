package ecommerce.modules.wishlist.dto;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import ecommerce.modules.wishlist.entity.WishlistPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WishlistItemDto {

    private UUID id;
    private UUID userId;
    private ProductSummary product;
    private String notes;
    private WishlistPriority priority;
    private Integer desiredQuantity;

    // Price tracking
    private BigDecimal priceWhenAdded;
    private BigDecimal currentPrice;
    private BigDecimal priceDifference;
    private Boolean isPriceDropped;
    private BigDecimal targetPrice;

    // Notifications
    private Boolean notifyOnPriceDrop;
    private Boolean notifyOnStock;
    private Boolean shouldNotifyPriceDrop;
    private Boolean shouldNotifyStock;

    // Status
    private Boolean purchased;
    private Boolean isPublic;
    private Boolean inStock;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime addedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime purchasedAt;

    /**
     * Nested product summary info
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSummary {
        private UUID id;
        private String name;
        private String slug;
        private String sku;
        private BigDecimal price;
        private BigDecimal discountPrice;
        private String imageUrl;
        private String categoryName;
        private Boolean inStock;
        private Integer availableQuantity;
        private String inventoryStatus;
    }
}

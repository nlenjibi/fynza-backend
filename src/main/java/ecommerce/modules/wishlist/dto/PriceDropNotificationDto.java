package ecommerce.modules.wishlist.dto;


import com.fasterxml.jackson.annotation.JsonFormat;
import ecommerce.common.validation.ValidPriceRange;
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
@ValidPriceRange
public class PriceDropNotificationDto {

    private Long wishlistItemId;
    private UUID productId;
    private String productName;
    private String productSlug;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private BigDecimal priceDrop;
    private BigDecimal priceDropPercentage;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime detectedAt;
}

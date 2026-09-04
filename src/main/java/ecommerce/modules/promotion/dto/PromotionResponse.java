package ecommerce.modules.promotion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionResponse {
    private UUID id;
    private String name;
    private String description;
    private String bannerImage;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer maxUses;
    private Integer currentUses;
    private Integer maxUsesPerUser;
    private Boolean isExclusive;
    private Boolean isFeatured;
    private String termsConditions;
    private String status;
    private Boolean isParticipating;
}

package ecommerce.modules.follow.dto;

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
public class FollowedStoreResponse {
    private UUID id;
    private UUID sellerId;
    private String storeName;
    private String storeLogo;
    private Boolean isVerified;
    private BigDecimal rating;
    private Integer reviewCount;
    private String location;
    private Integer productCount;
    private LocalDateTime followedDate;
}

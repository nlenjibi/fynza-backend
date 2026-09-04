package ecommerce.modules.seller.dto;

import ecommerce.common.enums.Region;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreResponse {
    private UUID id;
    private String storeName;
    private String storeDescription;
    private String storeWebsite;
    private String storeLogo;
    private String storeBanner;
    private String email;
    private String phone;
    private Region region;
    private String city;
    private String businessAddress;
    private String workingHours;
    private String facebookUrl;
    private String instagramUrl;
    private String twitterUrl;
    private BigDecimal rating;
    private Integer totalReviews;
    private Integer totalProducts;
    private Integer totalSales;
    private BigDecimal totalRevenue;
    private String verificationStatus;
    private String businessRegistration;
    private String bankName;
}

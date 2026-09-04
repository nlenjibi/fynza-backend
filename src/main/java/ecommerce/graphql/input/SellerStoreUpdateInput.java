package ecommerce.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerStoreUpdateInput {
    private String storeName;
    private String storeDescription;
    private String storeWebsite;
    private String storeLogo;
    private String storeBanner;
    private String email;
    private String phone;
    private String region;
    private String city;
    private String businessAddress;
    private String workingHours;
    private String facebookUrl;
    private String instagramUrl;
    private String twitterUrl;
    private String businessRegistration;
    private String bankName;
    private String accountHolderName;
    private String accountNumber;
}

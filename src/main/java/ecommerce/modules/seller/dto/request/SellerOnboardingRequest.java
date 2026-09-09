package ecommerce.modules.seller.dto.request;

import lombok.Data;

@Data
public class SellerOnboardingRequest {

    private String legalName;
    private String businessName;
    private String businessType;
    private String registrationNumber;
    private String taxIdentifier;
    private String description;
    private String website;
    private String email;
    private String phone;
    private String country;
    private String region;
    private String city;
    private String address;
}

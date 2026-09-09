package ecommerce.modules.seller.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SellerBusinessResponse {

    private UUID publicId;
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
    private Instant createdAt;
    private Instant updatedAt;
}

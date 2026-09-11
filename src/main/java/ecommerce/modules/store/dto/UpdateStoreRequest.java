package ecommerce.modules.store.dto;

import ecommerce.common.enums.Region;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UpdateStoreRequest {
    @NotBlank(message = "Store name is required")
    @Size(max = 255, message = "Store name must be less than 255 characters")
    private String storeName;
    private String storeDescription;
    @Size(max = 500) private String storeWebsite;
    @Size(max = 500) private String storeLogo;
    @Size(max = 500) private String storeBanner;
    @Email(message = "Invalid email format") private String email;
    @Size(max = 50) private String phone;
    private Region region;
    @Size(max = 100) private String city;
    @Size(max = 500) private String businessAddress;
    @Size(max = 255) private String workingHours;
    @Size(max = 500) private String facebookUrl;
    @Size(max = 500) private String instagramUrl;
    @Size(max = 500) private String twitterUrl;
    private String businessRegistration;
    private String bankName;
    private String accountHolderName;
    private String accountNumber;
}

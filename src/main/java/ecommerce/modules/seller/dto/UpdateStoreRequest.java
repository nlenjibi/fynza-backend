package ecommerce.modules.seller.dto;

import ecommerce.common.enums.Region;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStoreRequest {
    
    @NotBlank(message = "Store name is required")
    @Size(max = 255, message = "Store name must be less than 255 characters")
    private String storeName;
    
    private String storeDescription;
    
    @Size(max = 500, message = "Website must be less than 500 characters")
    private String storeWebsite;
    
    @Size(max = 500, message = "Logo URL must be less than 500 characters")
    private String storeLogo;
    
    @Size(max = 500, message = "Banner URL must be less than 500 characters")
    private String storeBanner;
    
    @Email(message = "Invalid email format")
    private String email;
    
    @Size(max = 50, message = "Phone must be less than 50 characters")
    private String phone;
    
    private Region region;
    
    @Size(max = 100, message = "City must be less than 100 characters")
    private String city;
    
    @Size(max = 500, message = "Business address must be less than 500 characters")
    private String businessAddress;
    
    @Size(max = 255, message = "Working hours must be less than 255 characters")
    private String workingHours;
    
    @Size(max = 500, message = "Facebook URL must be less than 500 characters")
    private String facebookUrl;
    
    @Size(max = 500, message = "Instagram URL must be less than 500 characters")
    private String instagramUrl;
    
    @Size(max = 500, message = "Twitter URL must be less than 500 characters")
    private String twitterUrl;
    
    private String businessRegistration;
    
    private String bankName;
    
    private String accountHolderName;
    
    private String accountNumber;
}

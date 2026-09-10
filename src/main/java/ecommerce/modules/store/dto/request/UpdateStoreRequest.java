package ecommerce.modules.store.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateStoreRequest {

    @Size(min = 2, max = 100)
    private String storeName;

    @Size(max = 1000)
    private String description;

    private String logoMediaId;

    private String bannerMediaId;

    @Email
    @Size(max = 255)
    private String businessEmail;

    @Size(max = 30)
    private String businessPhone;

    @Size(max = 255)
    private String website;
}

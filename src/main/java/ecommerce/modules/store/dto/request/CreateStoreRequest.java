package ecommerce.modules.store.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateStoreRequest {

    @NotBlank
    @Size(min = 2, max = 100)
    private String storeName;

    @Size(max = 120)
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must be lowercase alphanumeric with hyphens")
    private String slug;

    @Size(max = 1000)
    private String description;

    @Email
    @Size(max = 255)
    private String businessEmail;

    @Size(max = 30)
    private String businessPhone;

    @Size(max = 255)
    private String website;
}

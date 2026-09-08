package ecommerce.modules.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {

    @Size(min = 1, max = 100)
    private String firstName;

    @Size(min = 1, max = 100)
    private String lastName;

    @Size(max = 100)
    private String displayName;

    @Pattern(regexp = "^\\+?[1-9]\\d{6,14}$", message = "Invalid phone number format")
    private String phone;

    @Size(max = 500)
    private String avatarUrl;

    private LocalDate dateOfBirth;

    @Pattern(regexp = "^[a-z]{2,8}$", message = "Invalid language code")
    private String language;

    @Size(max = 50)
    private String timezone;

    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code")
    private String currency;
}

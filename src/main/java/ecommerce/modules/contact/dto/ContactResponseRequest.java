package ecommerce.modules.contact.dto;

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
public class ContactResponseRequest {

    @NotBlank(message = "Admin response is required")
    @Size(min = 10, max = 5000, message = "Response must be between 10 and 5000 characters")
    private String adminResponse;

    @NotBlank(message = "Admin ID is required")
    private String adminId;
}

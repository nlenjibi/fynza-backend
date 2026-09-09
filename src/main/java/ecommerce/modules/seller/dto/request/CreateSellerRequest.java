package ecommerce.modules.seller.dto.request;

import ecommerce.modules.seller.enums.SellerType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateSellerRequest {

    @NotBlank
    private String displayName;

    private SellerType sellerType;
}

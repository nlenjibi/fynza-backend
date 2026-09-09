package ecommerce.modules.seller.dto.request;

import ecommerce.common.enums.SellerStatus;
import ecommerce.modules.seller.enums.SellerType;
import lombok.Data;

@Data
public class SellerSearchRequest {

    private String query;
    private SellerStatus status;
    private SellerType sellerType;
}

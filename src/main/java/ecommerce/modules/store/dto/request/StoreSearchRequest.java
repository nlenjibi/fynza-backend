package ecommerce.modules.store.dto.request;

import ecommerce.modules.store.enums.StoreStatus;
import ecommerce.modules.store.enums.StoreVisibility;
import lombok.Data;

@Data
public class StoreSearchRequest {

    private String query;
    private StoreStatus status;
    private StoreVisibility visibility;
    private Boolean isActive;
}

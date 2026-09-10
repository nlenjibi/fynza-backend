package ecommerce.modules.store.dto.request;

import ecommerce.modules.store.enums.StoreVisibility;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StoreVisibilityRequest {

    @NotNull
    private StoreVisibility visibility;
}

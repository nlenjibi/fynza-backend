package ecommerce.modules.store.exception;

import ecommerce.common.exception.BadRequestException;
import ecommerce.modules.store.enums.StoreStatus;

public class StoreStatusTransitionException extends BadRequestException {

    public StoreStatusTransitionException(StoreStatus from, StoreStatus to) {
        super("Invalid store status transition: " + from + " → " + to);
    }
}

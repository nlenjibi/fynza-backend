package ecommerce.modules.seller.exception;

import ecommerce.common.enums.SellerStatus;

public class SellerStatusTransitionException extends RuntimeException {

    public SellerStatusTransitionException(SellerStatus from, SellerStatus to) {
        super("Invalid seller status transition: " + from + " → " + to);
    }
}

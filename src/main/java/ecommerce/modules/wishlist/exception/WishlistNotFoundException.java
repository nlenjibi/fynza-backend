package ecommerce.modules.wishlist.exception;

import ecommerce.common.exception.FynzaException;
import org.springframework.http.HttpStatus;

public class WishlistNotFoundException extends FynzaException {

    public WishlistNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "WISHLIST_NOT_FOUND");
    }
}

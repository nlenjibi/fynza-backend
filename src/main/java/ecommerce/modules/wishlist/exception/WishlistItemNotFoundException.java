package ecommerce.modules.wishlist.exception;

import ecommerce.common.exception.FynzaException;
import org.springframework.http.HttpStatus;

public class WishlistItemNotFoundException extends FynzaException {

    public WishlistItemNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "WISHLIST_ITEM_NOT_FOUND");
    }
}

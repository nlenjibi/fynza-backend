package ecommerce.modules.wishlist.exception;

import ecommerce.common.exception.FynzaException;
import org.springframework.http.HttpStatus;

public class WishlistItemAlreadyExistsException extends FynzaException {

    public WishlistItemAlreadyExistsException() {
        super("Item already in wishlist", HttpStatus.CONFLICT, "WISHLIST_ITEM_EXISTS");
    }
}

package ecommerce.modules.wishlist.exception;

import ecommerce.common.exception.FynzaException;
import org.springframework.http.HttpStatus;

public class WishlistAccessDeniedException extends FynzaException {

    public WishlistAccessDeniedException() {
        super("Access denied to this wishlist", HttpStatus.FORBIDDEN, "WISHLIST_ACCESS_DENIED");
    }
}

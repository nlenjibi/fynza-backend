package ecommerce.modules.cart.service;

import ecommerce.modules.cart.dto.CartValidateRequest;
import ecommerce.modules.cart.dto.CartValidateResponse;

public interface CartValidationService {

    CartValidateResponse validate(CartValidateRequest request);
}

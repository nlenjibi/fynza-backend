package ecommerce.common.validation;

import ecommerce.modules.wishlist.dto.request.AddWishlistItemRequest;
import ecommerce.modules.wishlist.dto.request.CreateWishlistRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ValidationLogicTest {

    private Validator validator;

    @BeforeEach
    public void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void testCreateWishlistRequest_Valid() {
        CreateWishlistRequest req = new CreateWishlistRequest();
        req.setName("My Wishlist");

        Set<ConstraintViolation<CreateWishlistRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty(), "Should have no violations for valid request");
    }

    @Test
    public void testCreateWishlistRequest_BlankName_Invalid() {
        CreateWishlistRequest req = new CreateWishlistRequest();
        req.setName("");

        Set<ConstraintViolation<CreateWishlistRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "Should have violations when name is blank");
    }

    @Test
    public void testAddWishlistItemRequest_Valid() {
        AddWishlistItemRequest req = new AddWishlistItemRequest();
        req.setProductId(UUID.randomUUID());

        Set<ConstraintViolation<AddWishlistItemRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty(), "Should have no violations when productId is present");
    }

    @Test
    public void testAddWishlistItemRequest_NullProductId_Invalid() {
        AddWishlistItemRequest req = new AddWishlistItemRequest();
        req.setProductId(null);

        Set<ConstraintViolation<AddWishlistItemRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "Should have violations when productId is null");
    }
}

package ecommerce.common.validation;

import ecommerce.modules.product.dto.CreateProductRequest;
import ecommerce.modules.product.entity.Product;
import ecommerce.modules.wishlist.dto.PriceDropNotificationDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

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
    public void testProductPriceRangeValid() {
        Product product = Product.builder()
                .name("Test Product")
                .price(new BigDecimal("80.00"))
                .originalPrice(new BigDecimal("100.00"))
                .build();

        Set<ConstraintViolation<Product>> violations = validator.validate(product);
        assertTrue(violations.isEmpty(), "Should have no violations for valid price range");
    }

    @Test
    public void testProductPriceRangeInvalid() {
        Product product = Product.builder()
                .name("Test Product")
                .price(new BigDecimal("120.00"))
                .originalPrice(new BigDecimal("100.00"))
                .build();

        Set<ConstraintViolation<Product>> violations = validator.validate(product);
        assertFalse(violations.isEmpty(), "Should have violations for price > originalPrice");
    }

    @Test
    public void testCreateProductRequestPriceRangeValid() {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Test Product")
                .price(new BigDecimal("80.00"))
                .originalPrice(new BigDecimal("100.00"))
                .categoryId(java.util.UUID.randomUUID())
                .build();

        Set<ConstraintViolation<CreateProductRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Should have no violations for valid range");
    }

    @Test
    public void testCreateProductRequestPriceRangeInvalid() {
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Test Product")
                .price(new BigDecimal("120.00"))
                .originalPrice(new BigDecimal("100.00"))
                .categoryId(java.util.UUID.randomUUID())
                .build();

        Set<ConstraintViolation<CreateProductRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Should have violations for price > originalPrice");
    }

    @Test
    public void testPriceDropNotificationValid() {
        PriceDropNotificationDto dto = PriceDropNotificationDto.builder()
                .productId(java.util.UUID.randomUUID())
                .oldPrice(new BigDecimal("100.00"))
                .newPrice(new BigDecimal("80.00"))
                .build();

        Set<ConstraintViolation<PriceDropNotificationDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Should have no violations for valid price drop");
    }

    @Test
    public void testPriceDropNotificationInvalid() {
        PriceDropNotificationDto dto = PriceDropNotificationDto.builder()
                .productId(java.util.UUID.randomUUID())
                .oldPrice(new BigDecimal("100.00"))
                .newPrice(new BigDecimal("120.00"))
                .build();

        Set<ConstraintViolation<PriceDropNotificationDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty(), "Should have violations for newPrice > oldPrice");
    }
}

package ecommerce.common.validation;

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

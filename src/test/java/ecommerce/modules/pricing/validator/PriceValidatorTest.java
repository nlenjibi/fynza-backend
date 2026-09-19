package ecommerce.modules.pricing.validator;

import ecommerce.modules.pricing.dto.request.CreatePriceRequest;
import ecommerce.modules.pricing.dto.request.UpdatePriceRequest;
import ecommerce.modules.pricing.entity.Price;
import ecommerce.modules.pricing.enums.PriceStatus;
import ecommerce.modules.pricing.enums.SupportedCurrency;
import ecommerce.modules.pricing.exception.InvalidPriceException;
import ecommerce.modules.pricing.exception.PriceOwnershipException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PriceValidator")
class PriceValidatorTest {

    private PriceValidator validator;
    private UUID ownerActorId;
    private UUID strangerActorId;
    private UUID pricePublicId;

    @BeforeEach
    void setUp() {
        validator       = new PriceValidator();
        ownerActorId    = UUID.randomUUID();
        strangerActorId = UUID.randomUUID();
        pricePublicId   = UUID.randomUUID();
    }

    private Price buildPrice(UUID createdBy) {
        return Price.builder()
                .id(1L)
                .publicId(pricePublicId)
                .productId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .currency(SupportedCurrency.GHS)
                .status(PriceStatus.DRAFT)
                .createdBy(createdBy)
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("validateCreate")
    class ValidateCreate {

        @Test
        @DisplayName("passes when saleAmount is null")
        void validateCreate_nullSaleAmount_passes() {
            CreatePriceRequest request = CreatePriceRequest.builder()
                    .productId(UUID.randomUUID())
                    .amount(new BigDecimal("50.00"))
                    .currency(SupportedCurrency.GHS)
                    .build();

            assertThatCode(() -> validator.validateCreate(request)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("passes when saleAmount equals amount")
        void validateCreate_saleAmountEqualsAmount_passes() {
            CreatePriceRequest request = CreatePriceRequest.builder()
                    .productId(UUID.randomUUID())
                    .amount(new BigDecimal("100.00"))
                    .saleAmount(new BigDecimal("100.00"))
                    .currency(SupportedCurrency.GHS)
                    .build();

            assertThatCode(() -> validator.validateCreate(request)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("passes when saleAmount is less than amount")
        void validateCreate_saleAmountLessThanAmount_passes() {
            CreatePriceRequest request = CreatePriceRequest.builder()
                    .productId(UUID.randomUUID())
                    .amount(new BigDecimal("100.00"))
                    .saleAmount(new BigDecimal("80.00"))
                    .currency(SupportedCurrency.GHS)
                    .build();

            assertThatCode(() -> validator.validateCreate(request)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("throws InvalidPriceException when saleAmount exceeds amount")
        void validateCreate_saleAmountExceedsAmount_throwsInvalidPriceException() {
            CreatePriceRequest request = CreatePriceRequest.builder()
                    .productId(UUID.randomUUID())
                    .amount(new BigDecimal("50.00"))
                    .saleAmount(new BigDecimal("99.99"))
                    .currency(SupportedCurrency.GHS)
                    .build();

            assertThatThrownBy(() -> validator.validateCreate(request))
                    .isInstanceOf(InvalidPriceException.class)
                    .hasMessageContaining("saleAmount must not exceed amount");
        }

        @Test
        @DisplayName("passes when both dates are null")
        void validateCreate_nullDates_passes() {
            CreatePriceRequest request = CreatePriceRequest.builder()
                    .productId(UUID.randomUUID())
                    .amount(new BigDecimal("50.00"))
                    .currency(SupportedCurrency.GHS)
                    .build();

            assertThatCode(() -> validator.validateCreate(request)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("throws InvalidPriceException when validUntil is before validFrom")
        void validateCreate_validUntilBeforeValidFrom_throwsInvalidPriceException() {
            Instant now = Instant.now();
            CreatePriceRequest request = CreatePriceRequest.builder()
                    .productId(UUID.randomUUID())
                    .amount(new BigDecimal("50.00"))
                    .currency(SupportedCurrency.GHS)
                    .validFrom(now.plusSeconds(3600))
                    .validUntil(now)
                    .build();

            assertThatThrownBy(() -> validator.validateCreate(request))
                    .isInstanceOf(InvalidPriceException.class)
                    .hasMessageContaining("validUntil must be after validFrom");
        }

        @Test
        @DisplayName("throws InvalidPriceException when validUntil equals validFrom")
        void validateCreate_validUntilEqualsValidFrom_throwsInvalidPriceException() {
            Instant fixed = Instant.parse("2026-01-01T00:00:00Z");
            CreatePriceRequest request = CreatePriceRequest.builder()
                    .productId(UUID.randomUUID())
                    .amount(new BigDecimal("50.00"))
                    .currency(SupportedCurrency.GHS)
                    .validFrom(fixed)
                    .validUntil(fixed)
                    .build();

            assertThatThrownBy(() -> validator.validateCreate(request))
                    .isInstanceOf(InvalidPriceException.class)
                    .hasMessageContaining("validUntil must be after validFrom");
        }

        @Test
        @DisplayName("passes when validUntil is strictly after validFrom")
        void validateCreate_validUntilAfterValidFrom_passes() {
            Instant now = Instant.now();
            CreatePriceRequest request = CreatePriceRequest.builder()
                    .productId(UUID.randomUUID())
                    .amount(new BigDecimal("50.00"))
                    .currency(SupportedCurrency.GHS)
                    .validFrom(now)
                    .validUntil(now.plusSeconds(86400))
                    .build();

            assertThatCode(() -> validator.validateCreate(request)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("validateUpdate")
    class ValidateUpdate {

        @Test
        @DisplayName("passes when request is entirely empty — existing values used and valid")
        void validateUpdate_emptyRequest_passes() {
            Price existing = buildPrice(ownerActorId);
            UpdatePriceRequest request = UpdatePriceRequest.builder().build();

            assertThatCode(() -> validator.validateUpdate(request, existing)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("passes when new amount is larger and existing saleAmount stays below")
        void validateUpdate_newAmountLargerThanExistingSale_passes() {
            Price existing = Price.builder()
                    .id(1L).publicId(pricePublicId).productId(UUID.randomUUID())
                    .amount(new BigDecimal("80.00")).saleAmount(new BigDecimal("60.00"))
                    .currency(SupportedCurrency.GHS).status(PriceStatus.DRAFT)
                    .createdBy(ownerActorId).isActive(true).build();

            UpdatePriceRequest request = UpdatePriceRequest.builder()
                    .amount(new BigDecimal("100.00"))
                    .build();

            assertThatCode(() -> validator.validateUpdate(request, existing)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("throws InvalidPriceException when merged saleAmount exceeds merged amount")
        void validateUpdate_mergedSaleExceedsAmount_throwsInvalidPriceException() {
            Price existing = Price.builder()
                    .id(1L).publicId(pricePublicId).productId(UUID.randomUUID())
                    .amount(new BigDecimal("200.00")).saleAmount(new BigDecimal("180.00"))
                    .currency(SupportedCurrency.GHS).status(PriceStatus.DRAFT)
                    .createdBy(ownerActorId).isActive(true).build();

            UpdatePriceRequest request = UpdatePriceRequest.builder()
                    .amount(new BigDecimal("50.00"))
                    .build();

            assertThatThrownBy(() -> validator.validateUpdate(request, existing))
                    .isInstanceOf(InvalidPriceException.class)
                    .hasMessageContaining("saleAmount must not exceed amount");
        }

        @Test
        @DisplayName("throws InvalidPriceException when both dates in request and validUntil not after validFrom")
        void validateUpdate_invalidDateRange_throwsInvalidPriceException() {
            Price existing = buildPrice(ownerActorId);
            Instant now = Instant.now();
            UpdatePriceRequest request = UpdatePriceRequest.builder()
                    .validFrom(now.plusSeconds(7200))
                    .validUntil(now)
                    .build();

            assertThatThrownBy(() -> validator.validateUpdate(request, existing))
                    .isInstanceOf(InvalidPriceException.class)
                    .hasMessageContaining("validUntil must be after validFrom");
        }

        @Test
        @DisplayName("passes when only validFrom is provided — no cross-check performed")
        void validateUpdate_onlyValidFromProvided_passes() {
            Price existing = buildPrice(ownerActorId);
            UpdatePriceRequest request = UpdatePriceRequest.builder()
                    .validFrom(Instant.now())
                    .build();

            assertThatCode(() -> validator.validateUpdate(request, existing)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("assertOwner")
    class AssertOwner {

        @Test
        @DisplayName("passes silently when actorId matches price.createdBy")
        void assertOwner_sameActor_passes() {
            Price price = buildPrice(ownerActorId);
            assertThatCode(() -> validator.assertOwner(price, ownerActorId)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("throws PriceOwnershipException when actorId differs from price.createdBy")
        void assertOwner_differentActor_throwsPriceOwnershipException() {
            Price price = buildPrice(ownerActorId);
            assertThatThrownBy(() -> validator.assertOwner(price, strangerActorId))
                    .isInstanceOf(PriceOwnershipException.class)
                    .hasMessageContaining(pricePublicId.toString());
        }

        @Test
        @DisplayName("exception message contains the specific price publicId")
        void assertOwner_exceptionContainsSpecificPricePublicId() {
            UUID specificPublicId = UUID.randomUUID();
            Price price = Price.builder()
                    .id(2L).publicId(specificPublicId).productId(UUID.randomUUID())
                    .amount(new BigDecimal("10.00")).currency(SupportedCurrency.GHS)
                    .status(PriceStatus.DRAFT).createdBy(ownerActorId).isActive(true).build();

            assertThatThrownBy(() -> validator.assertOwner(price, strangerActorId))
                    .isInstanceOf(PriceOwnershipException.class)
                    .hasMessageContaining(specificPublicId.toString());
        }
    }
}

package ecommerce.modules.seller.validation;

import ecommerce.common.enums.SellerStatus;
import ecommerce.modules.seller.exception.SellerStatusTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SellerStatusTransitionValidator Tests")
class SellerStatusTransitionValidatorTest {

    private SellerStatusTransitionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SellerStatusTransitionValidator();
    }

    @Nested
    @DisplayName("Valid transitions")
    class ValidTransitions {

        @Test
        @DisplayName("DRAFT → PENDING_VERIFICATION")
        void draft_to_pendingVerification() {
            assertThatCode(() -> validator.validate(SellerStatus.DRAFT, SellerStatus.PENDING_VERIFICATION))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("PENDING_VERIFICATION → UNDER_REVIEW")
        void pendingVerification_to_underReview() {
            assertThatCode(() -> validator.validate(SellerStatus.PENDING_VERIFICATION, SellerStatus.UNDER_REVIEW))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("UNDER_REVIEW → ACTIVE")
        void underReview_to_active() {
            assertThatCode(() -> validator.validate(SellerStatus.UNDER_REVIEW, SellerStatus.ACTIVE))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("UNDER_REVIEW → REJECTED")
        void underReview_to_rejected() {
            assertThatCode(() -> validator.validate(SellerStatus.UNDER_REVIEW, SellerStatus.REJECTED))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("ACTIVE → SUSPENDED")
        void active_to_suspended() {
            assertThatCode(() -> validator.validate(SellerStatus.ACTIVE, SellerStatus.SUSPENDED))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("ACTIVE → BLOCKED")
        void active_to_blocked() {
            assertThatCode(() -> validator.validate(SellerStatus.ACTIVE, SellerStatus.BLOCKED))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("ACTIVE → CLOSED")
        void active_to_closed() {
            assertThatCode(() -> validator.validate(SellerStatus.ACTIVE, SellerStatus.CLOSED))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("SUSPENDED → ACTIVE")
        void suspended_to_active() {
            assertThatCode(() -> validator.validate(SellerStatus.SUSPENDED, SellerStatus.ACTIVE))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("SUSPENDED → BLOCKED")
        void suspended_to_blocked() {
            assertThatCode(() -> validator.validate(SellerStatus.SUSPENDED, SellerStatus.BLOCKED))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Invalid transitions")
    class InvalidTransitions {

        @Test
        @DisplayName("DRAFT → ACTIVE throws")
        void draft_to_active_throws() {
            assertThatThrownBy(() -> validator.validate(SellerStatus.DRAFT, SellerStatus.ACTIVE))
                    .isInstanceOf(SellerStatusTransitionException.class)
                    .hasMessageContaining("DRAFT")
                    .hasMessageContaining("ACTIVE");
        }

        @Test
        @DisplayName("REJECTED → ACTIVE throws")
        void rejected_to_active_throws() {
            assertThatThrownBy(() -> validator.validate(SellerStatus.REJECTED, SellerStatus.ACTIVE))
                    .isInstanceOf(SellerStatusTransitionException.class);
        }

        @Test
        @DisplayName("BLOCKED → ACTIVE throws")
        void blocked_to_active_throws() {
            assertThatThrownBy(() -> validator.validate(SellerStatus.BLOCKED, SellerStatus.ACTIVE))
                    .isInstanceOf(SellerStatusTransitionException.class);
        }

        @Test
        @DisplayName("CLOSED → ACTIVE throws")
        void closed_to_active_throws() {
            assertThatThrownBy(() -> validator.validate(SellerStatus.CLOSED, SellerStatus.ACTIVE))
                    .isInstanceOf(SellerStatusTransitionException.class);
        }

        @Test
        @DisplayName("Self-transition ACTIVE → ACTIVE throws")
        void selfTransition_active_throws() {
            assertThatThrownBy(() -> validator.validate(SellerStatus.ACTIVE, SellerStatus.ACTIVE))
                    .isInstanceOf(SellerStatusTransitionException.class);
        }

        @Test
        @DisplayName("ACTIVE → DRAFT throws")
        void active_to_draft_throws() {
            assertThatThrownBy(() -> validator.validate(SellerStatus.ACTIVE, SellerStatus.DRAFT))
                    .isInstanceOf(SellerStatusTransitionException.class);
        }
    }
}

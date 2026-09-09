package ecommerce.modules.customer.validation;

import ecommerce.modules.customer.enums.CustomerStatus;
import ecommerce.modules.customer.exception.CustomerStatusTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerStatusTransitionValidator Tests")
class CustomerStatusTransitionValidatorTest {

    @InjectMocks
    private CustomerStatusTransitionValidator validator;

    // ── Valid transitions ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Valid transitions (must not throw)")
    class ValidTransitions {

        @Test
        @DisplayName("PROSPECT → ACTIVE is allowed")
        void prospectToActive_doesNotThrow() {
            assertThatCode(() -> validator.validate(CustomerStatus.PROSPECT, CustomerStatus.ACTIVE))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("PROSPECT → DELETED is allowed")
        void prospectToDeleted_doesNotThrow() {
            assertThatCode(() -> validator.validate(CustomerStatus.PROSPECT, CustomerStatus.DELETED))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("ACTIVE → INACTIVE is allowed")
        void activeToInactive_doesNotThrow() {
            assertThatCode(() -> validator.validate(CustomerStatus.ACTIVE, CustomerStatus.INACTIVE))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("ACTIVE → SUSPENDED is allowed")
        void activeToSuspended_doesNotThrow() {
            assertThatCode(() -> validator.validate(CustomerStatus.ACTIVE, CustomerStatus.SUSPENDED))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("ACTIVE → BLOCKED is allowed")
        void activeToBlocked_doesNotThrow() {
            assertThatCode(() -> validator.validate(CustomerStatus.ACTIVE, CustomerStatus.BLOCKED))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("ACTIVE → DELETED is allowed")
        void activeToDeleted_doesNotThrow() {
            assertThatCode(() -> validator.validate(CustomerStatus.ACTIVE, CustomerStatus.DELETED))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("INACTIVE → ACTIVE is allowed")
        void inactiveToActive_doesNotThrow() {
            assertThatCode(() -> validator.validate(CustomerStatus.INACTIVE, CustomerStatus.ACTIVE))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("INACTIVE → DELETED is allowed")
        void inactiveToDeleted_doesNotThrow() {
            assertThatCode(() -> validator.validate(CustomerStatus.INACTIVE, CustomerStatus.DELETED))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("SUSPENDED → ACTIVE is allowed")
        void suspendedToActive_doesNotThrow() {
            assertThatCode(() -> validator.validate(CustomerStatus.SUSPENDED, CustomerStatus.ACTIVE))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("SUSPENDED → BLOCKED is allowed")
        void suspendedToBlocked_doesNotThrow() {
            assertThatCode(() -> validator.validate(CustomerStatus.SUSPENDED, CustomerStatus.BLOCKED))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("SUSPENDED → DELETED is allowed")
        void suspendedToDeleted_doesNotThrow() {
            assertThatCode(() -> validator.validate(CustomerStatus.SUSPENDED, CustomerStatus.DELETED))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("BLOCKED → DELETED is allowed")
        void blockedToDeleted_doesNotThrow() {
            assertThatCode(() -> validator.validate(CustomerStatus.BLOCKED, CustomerStatus.DELETED))
                    .doesNotThrowAnyException();
        }
    }

    // ── Invalid transitions ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Invalid transitions (must throw CustomerStatusTransitionException)")
    class InvalidTransitions {

        @Test
        @DisplayName("PROSPECT → SUSPENDED is not allowed")
        void prospectToSuspended_throws() {
            assertThatThrownBy(() -> validator.validate(CustomerStatus.PROSPECT, CustomerStatus.SUSPENDED))
                    .isInstanceOf(CustomerStatusTransitionException.class)
                    .hasMessageContaining("PROSPECT")
                    .hasMessageContaining("SUSPENDED");
        }

        @Test
        @DisplayName("PROSPECT → BLOCKED is not allowed")
        void prospectToBlocked_throws() {
            assertThatThrownBy(() -> validator.validate(CustomerStatus.PROSPECT, CustomerStatus.BLOCKED))
                    .isInstanceOf(CustomerStatusTransitionException.class)
                    .hasMessageContaining("BLOCKED");
        }

        @Test
        @DisplayName("PROSPECT → INACTIVE is not allowed")
        void prospectToInactive_throws() {
            assertThatThrownBy(() -> validator.validate(CustomerStatus.PROSPECT, CustomerStatus.INACTIVE))
                    .isInstanceOf(CustomerStatusTransitionException.class)
                    .hasMessageContaining("INACTIVE");
        }

        @Test
        @DisplayName("ACTIVE → PROSPECT is not allowed")
        void activeToProspect_throws() {
            assertThatThrownBy(() -> validator.validate(CustomerStatus.ACTIVE, CustomerStatus.PROSPECT))
                    .isInstanceOf(CustomerStatusTransitionException.class);
        }

        @Test
        @DisplayName("INACTIVE → SUSPENDED is not allowed")
        void inactiveToSuspended_throws() {
            assertThatThrownBy(() -> validator.validate(CustomerStatus.INACTIVE, CustomerStatus.SUSPENDED))
                    .isInstanceOf(CustomerStatusTransitionException.class);
        }

        @Test
        @DisplayName("INACTIVE → BLOCKED is not allowed")
        void inactiveToBlocked_throws() {
            assertThatThrownBy(() -> validator.validate(CustomerStatus.INACTIVE, CustomerStatus.BLOCKED))
                    .isInstanceOf(CustomerStatusTransitionException.class);
        }

        @Test
        @DisplayName("SUSPENDED → INACTIVE is not allowed")
        void suspendedToInactive_throws() {
            assertThatThrownBy(() -> validator.validate(CustomerStatus.SUSPENDED, CustomerStatus.INACTIVE))
                    .isInstanceOf(CustomerStatusTransitionException.class);
        }

        @Test
        @DisplayName("SUSPENDED → PROSPECT is not allowed")
        void suspendedToProspect_throws() {
            assertThatThrownBy(() -> validator.validate(CustomerStatus.SUSPENDED, CustomerStatus.PROSPECT))
                    .isInstanceOf(CustomerStatusTransitionException.class);
        }

        @Test
        @DisplayName("BLOCKED → ACTIVE is not allowed")
        void blockedToActive_throws() {
            assertThatThrownBy(() -> validator.validate(CustomerStatus.BLOCKED, CustomerStatus.ACTIVE))
                    .isInstanceOf(CustomerStatusTransitionException.class)
                    .hasMessageContaining("BLOCKED")
                    .hasMessageContaining("ACTIVE");
        }

        @Test
        @DisplayName("BLOCKED → SUSPENDED is not allowed")
        void blockedToSuspended_throws() {
            assertThatThrownBy(() -> validator.validate(CustomerStatus.BLOCKED, CustomerStatus.SUSPENDED))
                    .isInstanceOf(CustomerStatusTransitionException.class);
        }

        @Test
        @DisplayName("BLOCKED → INACTIVE is not allowed")
        void blockedToInactive_throws() {
            assertThatThrownBy(() -> validator.validate(CustomerStatus.BLOCKED, CustomerStatus.INACTIVE))
                    .isInstanceOf(CustomerStatusTransitionException.class);
        }

        @Test
        @DisplayName("DELETED → ACTIVE is not allowed (terminal state)")
        void deletedToActive_throws() {
            assertThatThrownBy(() -> validator.validate(CustomerStatus.DELETED, CustomerStatus.ACTIVE))
                    .isInstanceOf(CustomerStatusTransitionException.class)
                    .hasMessageContaining("DELETED");
        }

        @Test
        @DisplayName("DELETED → SUSPENDED is not allowed (terminal state)")
        void deletedToSuspended_throws() {
            assertThatThrownBy(() -> validator.validate(CustomerStatus.DELETED, CustomerStatus.SUSPENDED))
                    .isInstanceOf(CustomerStatusTransitionException.class);
        }

        @Test
        @DisplayName("DELETED → BLOCKED is not allowed (terminal state)")
        void deletedToBlocked_throws() {
            assertThatThrownBy(() -> validator.validate(CustomerStatus.DELETED, CustomerStatus.BLOCKED))
                    .isInstanceOf(CustomerStatusTransitionException.class);
        }
    }

    // ── Same-status self-transitions ──────────────────────────────────────────

    @Nested
    @DisplayName("Self-transitions (same status → same status must throw)")
    class SelfTransitions {

        @Test
        @DisplayName("ACTIVE → ACTIVE is not in the allowed set and must throw")
        void activeToActive_throws() {
            assertThatThrownBy(() -> validator.validate(CustomerStatus.ACTIVE, CustomerStatus.ACTIVE))
                    .isInstanceOf(CustomerStatusTransitionException.class)
                    .hasMessageContaining("ACTIVE");
        }

        @Test
        @DisplayName("SUSPENDED → SUSPENDED is not allowed")
        void suspendedToSuspended_throws() {
            assertThatThrownBy(() -> validator.validate(CustomerStatus.SUSPENDED, CustomerStatus.SUSPENDED))
                    .isInstanceOf(CustomerStatusTransitionException.class);
        }

        @Test
        @DisplayName("BLOCKED → BLOCKED is not allowed")
        void blockedToBlocked_throws() {
            assertThatThrownBy(() -> validator.validate(CustomerStatus.BLOCKED, CustomerStatus.BLOCKED))
                    .isInstanceOf(CustomerStatusTransitionException.class);
        }

        @Test
        @DisplayName("DELETED → DELETED is not allowed")
        void deletedToDeleted_throws() {
            assertThatThrownBy(() -> validator.validate(CustomerStatus.DELETED, CustomerStatus.DELETED))
                    .isInstanceOf(CustomerStatusTransitionException.class);
        }
    }
}

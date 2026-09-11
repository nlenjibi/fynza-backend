package ecommerce.modules.store.validation;

import ecommerce.modules.store.enums.StoreStatus;
import ecommerce.modules.store.exception.StoreStatusTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoreStatusTransitionValidator Tests")
class StoreStatusTransitionValidatorTest {

    @InjectMocks
    private StoreStatusTransitionValidator validator;

    // -- Valid transitions --

    @Nested
    @DisplayName("Valid transitions (must not throw)")
    class ValidTransitions {

        @Test
        @DisplayName("DRAFT to PENDING_REVIEW is allowed")
        void draftToPendingReview_doesNotThrow() {
            assertThatCode(() -> validator.validate(StoreStatus.DRAFT, StoreStatus.PENDING_REVIEW))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("PENDING_REVIEW to ACTIVE is allowed")
        void pendingReviewToActive_doesNotThrow() {
            assertThatCode(() -> validator.validate(StoreStatus.PENDING_REVIEW, StoreStatus.ACTIVE))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("PENDING_REVIEW to DRAFT is allowed")
        void pendingReviewToDraft_doesNotThrow() {
            assertThatCode(() -> validator.validate(StoreStatus.PENDING_REVIEW, StoreStatus.DRAFT))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("ACTIVE to PAUSED is allowed")
        void activeToPaused_doesNotThrow() {
            assertThatCode(() -> validator.validate(StoreStatus.ACTIVE, StoreStatus.PAUSED))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("ACTIVE to SUSPENDED is allowed")
        void activeToSuspended_doesNotThrow() {
            assertThatCode(() -> validator.validate(StoreStatus.ACTIVE, StoreStatus.SUSPENDED))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("ACTIVE to CLOSED is allowed")
        void activeToClosed_doesNotThrow() {
            assertThatCode(() -> validator.validate(StoreStatus.ACTIVE, StoreStatus.CLOSED))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("PAUSED to ACTIVE is allowed")
        void pausedToActive_doesNotThrow() {
            assertThatCode(() -> validator.validate(StoreStatus.PAUSED, StoreStatus.ACTIVE))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("PAUSED to CLOSED is allowed")
        void pausedToClosed_doesNotThrow() {
            assertThatCode(() -> validator.validate(StoreStatus.PAUSED, StoreStatus.CLOSED))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("SUSPENDED to ACTIVE is allowed")
        void suspendedToActive_doesNotThrow() {
            assertThatCode(() -> validator.validate(StoreStatus.SUSPENDED, StoreStatus.ACTIVE))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("SUSPENDED to CLOSED is allowed")
        void suspendedToClosed_doesNotThrow() {
            assertThatCode(() -> validator.validate(StoreStatus.SUSPENDED, StoreStatus.CLOSED))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("CLOSED to ARCHIVED is allowed")
        void closedToArchived_doesNotThrow() {
            assertThatCode(() -> validator.validate(StoreStatus.CLOSED, StoreStatus.ARCHIVED))
                    .doesNotThrowAnyException();
        }
    }

    // -- Invalid transitions --

    @Nested
    @DisplayName("Invalid transitions (must throw StoreStatusTransitionException)")
    class InvalidTransitions {

        @Test
        @DisplayName("DRAFT to ACTIVE is not allowed")
        void draftToActive_throws() {
            assertThatThrownBy(() -> validator.validate(StoreStatus.DRAFT, StoreStatus.ACTIVE))
                    .isInstanceOf(StoreStatusTransitionException.class)
                    .hasMessageContaining("DRAFT")
                    .hasMessageContaining("ACTIVE");
        }

        @Test
        @DisplayName("DRAFT to SUSPENDED is not allowed")
        void draftToSuspended_throws() {
            assertThatThrownBy(() -> validator.validate(StoreStatus.DRAFT, StoreStatus.SUSPENDED))
                    .isInstanceOf(StoreStatusTransitionException.class)
                    .hasMessageContaining("DRAFT")
                    .hasMessageContaining("SUSPENDED");
        }

        @Test
        @DisplayName("DRAFT to PAUSED is not allowed")
        void draftToPaused_throws() {
            assertThatThrownBy(() -> validator.validate(StoreStatus.DRAFT, StoreStatus.PAUSED))
                    .isInstanceOf(StoreStatusTransitionException.class)
                    .hasMessageContaining("DRAFT");
        }

        @Test
        @DisplayName("DRAFT to CLOSED is not allowed")
        void draftToClosed_throws() {
            assertThatThrownBy(() -> validator.validate(StoreStatus.DRAFT, StoreStatus.CLOSED))
                    .isInstanceOf(StoreStatusTransitionException.class)
                    .hasMessageContaining("DRAFT");
        }

        @Test
        @DisplayName("DRAFT to ARCHIVED is not allowed")
        void draftToArchived_throws() {
            assertThatThrownBy(() -> validator.validate(StoreStatus.DRAFT, StoreStatus.ARCHIVED))
                    .isInstanceOf(StoreStatusTransitionException.class)
                    .hasMessageContaining("DRAFT");
        }

        @Test
        @DisplayName("ACTIVE to DRAFT is not allowed")
        void activeToDraft_throws() {
            assertThatThrownBy(() -> validator.validate(StoreStatus.ACTIVE, StoreStatus.DRAFT))
                    .isInstanceOf(StoreStatusTransitionException.class)
                    .hasMessageContaining("ACTIVE")
                    .hasMessageContaining("DRAFT");
        }

        @Test
        @DisplayName("ACTIVE to PENDING_REVIEW is not allowed")
        void activeToPendingReview_throws() {
            assertThatThrownBy(() -> validator.validate(StoreStatus.ACTIVE, StoreStatus.PENDING_REVIEW))
                    .isInstanceOf(StoreStatusTransitionException.class)
                    .hasMessageContaining("ACTIVE")
                    .hasMessageContaining("PENDING_REVIEW");
        }

        @Test
        @DisplayName("ACTIVE to ARCHIVED is not allowed")
        void activeToArchived_throws() {
            assertThatThrownBy(() -> validator.validate(StoreStatus.ACTIVE, StoreStatus.ARCHIVED))
                    .isInstanceOf(StoreStatusTransitionException.class)
                    .hasMessageContaining("ACTIVE");
        }

        @Test
        @DisplayName("ARCHIVED to ACTIVE is not allowed (terminal state)")
        void archivedToActive_throws() {
            assertThatThrownBy(() -> validator.validate(StoreStatus.ARCHIVED, StoreStatus.ACTIVE))
                    .isInstanceOf(StoreStatusTransitionException.class)
                    .hasMessageContaining("ARCHIVED");
        }

        @Test
        @DisplayName("ARCHIVED to DRAFT is not allowed (terminal state)")
        void archivedToDraft_throws() {
            assertThatThrownBy(() -> validator.validate(StoreStatus.ARCHIVED, StoreStatus.DRAFT))
                    .isInstanceOf(StoreStatusTransitionException.class)
                    .hasMessageContaining("ARCHIVED");
        }

        @Test
        @DisplayName("ARCHIVED to PENDING_REVIEW is not allowed (terminal state)")
        void archivedToPendingReview_throws() {
            assertThatThrownBy(() -> validator.validate(StoreStatus.ARCHIVED, StoreStatus.PENDING_REVIEW))
                    .isInstanceOf(StoreStatusTransitionException.class)
                    .hasMessageContaining("ARCHIVED");
        }

        @Test
        @DisplayName("ARCHIVED to PAUSED is not allowed (terminal state)")
        void archivedToPaused_throws() {
            assertThatThrownBy(() -> validator.validate(StoreStatus.ARCHIVED, StoreStatus.PAUSED))
                    .isInstanceOf(StoreStatusTransitionException.class)
                    .hasMessageContaining("ARCHIVED");
        }

        @Test
        @DisplayName("ARCHIVED to SUSPENDED is not allowed (terminal state)")
        void archivedToSuspended_throws() {
            assertThatThrownBy(() -> validator.validate(StoreStatus.ARCHIVED, StoreStatus.SUSPENDED))
                    .isInstanceOf(StoreStatusTransitionException.class)
                    .hasMessageContaining("ARCHIVED");
        }

        @Test
        @DisplayName("ARCHIVED to CLOSED is not allowed (terminal state)")
        void archivedToClosed_throws() {
            assertThatThrownBy(() -> validator.validate(StoreStatus.ARCHIVED, StoreStatus.CLOSED))
                    .isInstanceOf(StoreStatusTransitionException.class)
                    .hasMessageContaining("ARCHIVED");
        }
    }
}

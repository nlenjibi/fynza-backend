package ecommerce.modules.product.validation;

import ecommerce.common.enums.ProductStatus;
import ecommerce.modules.product.exception.ProductStatusTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ProductStatusTransitionValidator")
class ProductStatusTransitionValidatorTest {

    private final ProductStatusTransitionValidator validator = new ProductStatusTransitionValidator();

    @Nested
    @DisplayName("DRAFT transitions")
    class FromDraft {

        @Test
        @DisplayName("DRAFT → PENDING_REVIEW is allowed")
        void draft_toPendingReview_allowed() {
            assertThat(validator.isAllowed(ProductStatus.DRAFT, ProductStatus.PENDING_REVIEW)).isTrue();
        }

        @Test
        @DisplayName("DRAFT → ARCHIVED is allowed")
        void draft_toArchived_allowed() {
            assertThat(validator.isAllowed(ProductStatus.DRAFT, ProductStatus.ARCHIVED)).isTrue();
        }

        @Test
        @DisplayName("DRAFT → ACTIVE is forbidden")
        void draft_toActive_forbidden() {
            assertThatThrownBy(() -> validator.validate(ProductStatus.DRAFT, ProductStatus.ACTIVE))
                    .isInstanceOf(ProductStatusTransitionException.class);
        }
    }

    @Nested
    @DisplayName("ACTIVE transitions")
    class FromActive {

        @Test
        @DisplayName("ACTIVE → INACTIVE is allowed")
        void active_toInactive_allowed() {
            assertThat(validator.isAllowed(ProductStatus.ACTIVE, ProductStatus.INACTIVE)).isTrue();
        }

        @Test
        @DisplayName("ACTIVE → SUSPENDED is allowed")
        void active_toSuspended_allowed() {
            assertThat(validator.isAllowed(ProductStatus.ACTIVE, ProductStatus.SUSPENDED)).isTrue();
        }

        @Test
        @DisplayName("ACTIVE → ARCHIVED is allowed")
        void active_toArchived_allowed() {
            assertThat(validator.isAllowed(ProductStatus.ACTIVE, ProductStatus.ARCHIVED)).isTrue();
        }

        @Test
        @DisplayName("ACTIVE → DRAFT is forbidden")
        void active_toDraft_forbidden() {
            assertThatThrownBy(() -> validator.validate(ProductStatus.ACTIVE, ProductStatus.DRAFT))
                    .isInstanceOf(ProductStatusTransitionException.class);
        }
    }

    @Nested
    @DisplayName("DELETED is terminal")
    class Deleted {

        @ParameterizedTest
        @EnumSource(ProductStatus.class)
        @DisplayName("No transition out of DELETED is allowed")
        void deleted_toAny_forbidden(ProductStatus to) {
            assertThat(validator.isAllowed(ProductStatus.DELETED, to)).isFalse();
        }
    }

    @Nested
    @DisplayName("SUSPENDED transitions")
    class FromSuspended {

        @Test
        @DisplayName("SUSPENDED → ACTIVE is allowed (admin restore)")
        void suspended_toActive_allowed() {
            assertThat(validator.isAllowed(ProductStatus.SUSPENDED, ProductStatus.ACTIVE)).isTrue();
        }

        @Test
        @DisplayName("SUSPENDED → DRAFT is forbidden")
        void suspended_toDraft_forbidden() {
            assertThat(validator.isAllowed(ProductStatus.SUSPENDED, ProductStatus.DRAFT)).isFalse();
        }
    }
}

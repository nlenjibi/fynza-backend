package ecommerce.modules.customer.policy;

import ecommerce.common.exception.ForbiddenException;
import ecommerce.modules.customer.entity.Customer;
import ecommerce.modules.customer.enums.CustomerStatus;
import ecommerce.modules.customer.exception.CustomerNotFoundException;
import ecommerce.modules.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerOwnershipPolicy Tests")
class CustomerOwnershipPolicyTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerOwnershipPolicy policy;

    private UUID userId;
    private UUID customerPublicId;
    private Customer customer;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        customerPublicId = UUID.randomUUID();

        customer = Customer.builder()
                .userId(userId)
                .customerNumber("CUS-000001")
                .status(CustomerStatus.ACTIVE)
                .build();
    }

    // ── assertOwns ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("assertOwns(customerPublicId, actorUserId)")
    class AssertOwns {

        @Test
        @DisplayName("Returns customer when publicId matches and userId matches")
        void whenOwnershipMatches_returnsCustomer() {
            when(customerRepository.findByPublicId(customerPublicId)).thenReturn(Optional.of(customer));

            Customer result = policy.assertOwns(customerPublicId, userId);

            assertThat(result).isEqualTo(customer);
        }

        @Test
        @DisplayName("Throws CustomerNotFoundException when customer publicId not found")
        void whenCustomerNotFound_throwsCustomerNotFoundException() {
            when(customerRepository.findByPublicId(customerPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> policy.assertOwns(customerPublicId, userId))
                    .isInstanceOf(CustomerNotFoundException.class)
                    .hasMessageContaining(customerPublicId.toString());
        }

        @Test
        @DisplayName("Throws ForbiddenException when customer userId does not match actorUserId")
        void whenUserIdMismatch_throwsForbiddenException() {
            UUID differentUserId = UUID.randomUUID();
            when(customerRepository.findByPublicId(customerPublicId)).thenReturn(Optional.of(customer));

            assertThatThrownBy(() -> policy.assertOwns(customerPublicId, differentUserId))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Access denied");
        }
    }

    // ── resolveOwn ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("resolveOwn(actorUserId)")
    class ResolveOwn {

        @Test
        @DisplayName("Returns customer when record found for userId")
        void whenCustomerFound_returnsCustomer() {
            when(customerRepository.findByUserId(userId)).thenReturn(Optional.of(customer));

            Customer result = policy.resolveOwn(userId);

            assertThat(result).isEqualTo(customer);
            assertThat(result.getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Throws CustomerNotFoundException when no customer record found for userId")
        void whenNoCustomerForUser_throwsCustomerNotFoundException() {
            when(customerRepository.findByUserId(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> policy.resolveOwn(userId))
                    .isInstanceOf(CustomerNotFoundException.class)
                    .hasMessageContaining("No customer record found for current user");
        }
    }
}

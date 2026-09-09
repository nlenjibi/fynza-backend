package ecommerce.modules.customer.listener;

import ecommerce.common.enums.Role;
import ecommerce.common.event.user.UserRegisteredEvent;
import ecommerce.modules.customer.exception.CustomerAlreadyExistsException;
import ecommerce.modules.customer.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerProvisioningListener Tests")
class CustomerProvisioningListenerTest {

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private CustomerProvisioningListener listener;

    private UUID userId;
    private String email;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        email  = "alice@example.com";
    }

    // ── CUSTOMER role ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("When role is CUSTOMER")
    class CustomerRoleTests {

        @Test
        @DisplayName("Should call customerService.provision() with userId and email")
        void onUserRegistered_WithCustomerRole_CallsProvision() {
            UserRegisteredEvent event = new UserRegisteredEvent(
                    userId, email, "Alice Test", Role.CUSTOMER, false, "tok");

            listener.onUserRegistered(event);

            verify(customerService).provision(userId, email);
        }

        @Test
        @DisplayName("Should swallow CustomerAlreadyExistsException (idempotent re-delivery)")
        void onUserRegistered_WhenDuplicate_SwallowsException() {
            UserRegisteredEvent event = new UserRegisteredEvent(
                    userId, email, "Alice Test", Role.CUSTOMER, false, "tok");
            doThrow(new CustomerAlreadyExistsException(userId))
                    .when(customerService).provision(userId, email);

            // must not propagate
            listener.onUserRegistered(event);

            verify(customerService).provision(userId, email);
        }

        @Test
        @DisplayName("Should swallow generic exceptions and not rethrow")
        void onUserRegistered_WhenUnexpectedError_DoesNotPropagate() {
            UserRegisteredEvent event = new UserRegisteredEvent(
                    userId, email, "Alice Test", Role.CUSTOMER, false, "tok");
            doThrow(new RuntimeException("DB is down"))
                    .when(customerService).provision(userId, email);

            // must not propagate
            listener.onUserRegistered(event);

            verify(customerService).provision(userId, email);
        }
    }

    // ── Non-CUSTOMER roles ────────────────────────────────────────────────────

    @Nested
    @DisplayName("When role is not CUSTOMER")
    class NonCustomerRoleTests {

        @Test
        @DisplayName("Should not call provision() for SELLER role")
        void onUserRegistered_WithSellerRole_DoesNotCallProvision() {
            UserRegisteredEvent event = new UserRegisteredEvent(
                    userId, email, "Bob Seller", Role.SELLER, false, null);

            listener.onUserRegistered(event);

            verify(customerService, never()).provision(any(), any());
        }

        @Test
        @DisplayName("Should not call provision() for ADMIN role")
        void onUserRegistered_WithAdminRole_DoesNotCallProvision() {
            UserRegisteredEvent event = new UserRegisteredEvent(
                    userId, email, "Admin User", Role.ADMIN, false, null);

            listener.onUserRegistered(event);

            verify(customerService, never()).provision(any(), any());
        }
    }
}

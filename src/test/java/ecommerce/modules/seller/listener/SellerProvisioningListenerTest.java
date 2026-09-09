package ecommerce.modules.seller.listener;

import ecommerce.common.enums.Role;
import ecommerce.common.event.user.UserRegisteredEvent;
import ecommerce.modules.seller.exception.SellerAlreadyExistsException;
import ecommerce.modules.seller.service.SellerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SellerProvisioningListener Tests")
class SellerProvisioningListenerTest {

    @Mock  private SellerService sellerService;
    @InjectMocks private SellerProvisioningListener listener;

    @Test
    @DisplayName("SELLER role — calls sellerService.provision()")
    void onUserRegistered_sellerRole_callsProvision() {
        UUID userId = UUID.randomUUID();
        UserRegisteredEvent event = new UserRegisteredEvent(
                userId, "seller@example.com", "Jane Doe", Role.SELLER, false, "token");

        listener.onUserRegistered(event);

        verify(sellerService).provision(eq(userId), any(String.class));
    }

    @Test
    @DisplayName("CUSTOMER role — does not call provision()")
    void onUserRegistered_customerRole_doesNotCallProvision() {
        UserRegisteredEvent event = new UserRegisteredEvent(
                UUID.randomUUID(), "customer@example.com", "John Smith", Role.CUSTOMER, false, "token");

        listener.onUserRegistered(event);

        verify(sellerService, never()).provision(any(), any());
    }

    @Test
    @DisplayName("ADMIN role — does not call provision()")
    void onUserRegistered_adminRole_doesNotCallProvision() {
        UserRegisteredEvent event = new UserRegisteredEvent(
                UUID.randomUUID(), "admin@example.com", "Admin User", Role.ADMIN, false, "token");

        listener.onUserRegistered(event);

        verify(sellerService, never()).provision(any(), any());
    }

    @Test
    @DisplayName("SellerAlreadyExistsException is swallowed — no rethrow")
    void onUserRegistered_sellerAlreadyExists_swallowsException() {
        UUID userId = UUID.randomUUID();
        UserRegisteredEvent event = new UserRegisteredEvent(
                userId, "seller@example.com", "Jane Doe", Role.SELLER, false, "token");

        doThrow(new SellerAlreadyExistsException(userId))
                .when(sellerService).provision(any(), any());

        // Must not throw
        listener.onUserRegistered(event);
    }

    @Test
    @DisplayName("Generic exception is swallowed — no rethrow")
    void onUserRegistered_genericException_swallowsException() {
        UUID userId = UUID.randomUUID();
        UserRegisteredEvent event = new UserRegisteredEvent(
                userId, "seller@example.com", "Jane Doe", Role.SELLER, false, "token");

        doThrow(new RuntimeException("DB failure"))
                .when(sellerService).provision(any(), any());

        // Must not throw
        listener.onUserRegistered(event);
    }
}

package ecommerce.modules.customer.listener;

import ecommerce.common.enums.Role;
import ecommerce.common.event.user.UserRegisteredEvent;
import ecommerce.modules.customer.exception.CustomerAlreadyExistsException;
import ecommerce.modules.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerProvisioningListener {

    private final CustomerService customerService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUserRegistered(UserRegisteredEvent event) {
        if (event.role() != Role.CUSTOMER) {
            return;
        }
        try {
            customerService.provision(event.userId(), event.email());
        } catch (CustomerAlreadyExistsException ex) {
            log.warn("[CustomerProvisioning] Idempotent skip — customer already exists for userId={}", event.userId());
        } catch (Exception ex) {
            log.error("[CustomerProvisioning] Failed to provision customer for userId={}: {}", event.userId(), ex.getMessage(), ex);
        }
    }
}

package ecommerce.modules.seller.listener;

import ecommerce.common.enums.Role;
import ecommerce.common.event.user.UserRegisteredEvent;
import ecommerce.modules.seller.exception.SellerAlreadyExistsException;
import ecommerce.modules.seller.service.SellerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SellerProvisioningListener {

    private final SellerService sellerService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUserRegistered(UserRegisteredEvent event) {
        if (event.role() != Role.SELLER) {
            return;
        }
        try {
            sellerService.provision(event.userId(), event.fullName() + "'s Store");
            log.info("Seller provisioned for userId={}", event.userId());
        } catch (SellerAlreadyExistsException e) {
            log.warn("Seller already exists for userId={}, skipping provisioning", event.userId());
        } catch (Exception e) {
            log.error("Failed to provision seller for userId={}: {}", event.userId(), e.getMessage(), e);
        }
    }
}

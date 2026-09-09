package ecommerce.modules.customer.scheduler;

import ecommerce.modules.customer.entity.Customer;
import ecommerce.modules.customer.entity.CustomerStatusHistory;
import ecommerce.modules.customer.enums.CustomerStatus;
import ecommerce.modules.customer.repository.CustomerRepository;
import ecommerce.modules.customer.repository.CustomerStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerSuspensionExpiryScheduler {

    private final CustomerStatusHistoryRepository historyRepository;
    private final CustomerRepository              customerRepository;

    @Scheduled(cron = "${customer.suspension-expiry.cron:0 0 * * * *}")
    @Transactional
    public void expireSuspensions() {
        List<CustomerStatusHistory> expired =
                historyRepository.findExpiredSuspensions(CustomerStatus.SUSPENDED, Instant.now());

        for (CustomerStatusHistory history : expired) {
            customerRepository.findById(history.getCustomerId()).ifPresent(customer -> {
                if (customer.getStatus() == CustomerStatus.SUSPENDED) {
                    customer.setStatus(CustomerStatus.ACTIVE);
                    customerRepository.save(customer);
                    log.info("[SuspensionExpiry] Customer {} reactivated (suspension expired)", customer.getPublicId());
                }
            });
        }
    }
}

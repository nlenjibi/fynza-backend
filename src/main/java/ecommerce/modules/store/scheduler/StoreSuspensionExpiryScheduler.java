package ecommerce.modules.store.scheduler;

import ecommerce.modules.store.entity.StoreStatusHistory;
import ecommerce.modules.store.enums.StoreStatus;
import ecommerce.modules.store.repository.StoreRepository;
import ecommerce.modules.store.repository.StoreStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StoreSuspensionExpiryScheduler {

    private final StoreStatusHistoryRepository historyRepository;
    private final StoreRepository              storeRepository;

    @Scheduled(cron = "${store.suspension-expiry.cron:0 0 * * * *}")
    @Transactional
    public void expireSuspensions() {
        List<StoreStatusHistory> expired =
                historyRepository.findExpiredSuspensions(StoreStatus.SUSPENDED, Instant.now());

        for (StoreStatusHistory history : expired) {
            storeRepository.findById(history.getStoreId()).ifPresent(store -> {
                if (store.getStatus() == StoreStatus.SUSPENDED) {
                    store.setStatus(StoreStatus.ACTIVE);
                    storeRepository.save(store);
                    log.info("[SuspensionExpiry] Store {} reactivated (suspension expired)", store.getPublicId());
                }
            });
        }
    }
}

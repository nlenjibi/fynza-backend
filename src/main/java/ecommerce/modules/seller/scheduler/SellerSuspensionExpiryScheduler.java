package ecommerce.modules.seller.scheduler;

import ecommerce.common.enums.SellerStatus;
import ecommerce.modules.seller.entity.SellerStatusHistory;
import ecommerce.modules.seller.repository.SellerRepository;
import ecommerce.modules.seller.repository.SellerStatusHistoryRepository;
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
public class SellerSuspensionExpiryScheduler {

    private final SellerStatusHistoryRepository historyRepository;
    private final SellerRepository              sellerRepository;

    @Scheduled(cron = "${seller.suspension-expiry.cron:0 0 * * * *}")
    @Transactional
    public void expireSuspensions() {
        List<SellerStatusHistory> expired =
                historyRepository.findExpiredSuspensions(SellerStatus.SUSPENDED, Instant.now());

        for (SellerStatusHistory history : expired) {
            sellerRepository.findById(history.getSellerId()).ifPresent(seller -> {
                if (seller.getStatus() == SellerStatus.SUSPENDED) {
                    seller.setStatus(SellerStatus.ACTIVE);
                    sellerRepository.save(seller);
                    log.info("[SuspensionExpiry] Seller {} reactivated (suspension expired)", seller.getPublicId());
                }
            });
        }
    }
}

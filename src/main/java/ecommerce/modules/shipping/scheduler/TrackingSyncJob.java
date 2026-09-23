package ecommerce.modules.shipping.scheduler;

import ecommerce.modules.shipping.entity.Shipment;
import ecommerce.modules.shipping.enums.ShipmentStatus;
import ecommerce.modules.shipping.provider.ShippingProvider;
import ecommerce.modules.shipping.provider.ShippingProviderFactory;
import ecommerce.modules.shipping.provider.dto.TrackingResult;
import ecommerce.modules.shipping.repository.ShipmentRepository;
import ecommerce.modules.shipping.service.ShippingMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TrackingSyncJob {

    private static final EnumSet<ShipmentStatus> ACTIVE_STATUSES =
            EnumSet.of(ShipmentStatus.IN_TRANSIT, ShipmentStatus.OUT_FOR_DELIVERY);

    private final ShipmentRepository shipmentRepository;
    private final ShippingProviderFactory providerFactory;
    private final ShippingMetricsService metricsService;

    @Scheduled(cron = "${shipping.tracking-sync.cron:0 0/30 * * * *}")
    @Transactional(readOnly = true)
    public void syncTrackingStatuses() {
        Instant staleCutoff = Instant.now().minus(2, ChronoUnit.HOURS);
        List<Shipment> shipments = shipmentRepository.findByStatusInAndUpdatedAtBefore(ACTIVE_STATUSES, staleCutoff);

        if (shipments.isEmpty()) {
            log.debug("TrackingSyncJob: no stale in-transit shipments found");
            return;
        }

        log.info("TrackingSyncJob: syncing {} shipments not updated in last 2h", shipments.size());
        int synced = 0;

        for (Shipment shipment : shipments) {
            if (shipment.getTrackingNumber() == null) continue;
            try {
                ShippingProvider provider = providerFactory.getDefault();
                TrackingResult result = provider.getTracking(shipment.getTrackingNumber());
                if (result != null && result.getStatus() != null) {
                    log.info("TrackingSyncJob: shipment={} carrier_status={} local_status={}",
                            shipment.getPublicId(), result.getStatus(), shipment.getStatus());
                    synced++;
                }
            } catch (Exception e) {
                log.warn("TrackingSyncJob: failed to sync shipment={}: {}", shipment.getPublicId(), e.getMessage());
            }
        }

        log.info("TrackingSyncJob: completed — synced={} of total={}", synced, shipments.size());
    }
}

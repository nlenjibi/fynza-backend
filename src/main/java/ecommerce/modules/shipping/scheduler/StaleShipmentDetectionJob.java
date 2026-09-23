package ecommerce.modules.shipping.scheduler;

import ecommerce.modules.shipping.entity.Shipment;
import ecommerce.modules.shipping.enums.ShipmentStatus;
import ecommerce.modules.shipping.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StaleShipmentDetectionJob {

    private final ShipmentRepository shipmentRepository;

    @Scheduled(cron = "${shipping.stale-detection.cron:0 0 3 * * *}")
    @Transactional(readOnly = true)
    public void detectStaleShipments() {
        log.info("StaleShipmentDetectionJob: starting stale shipment scan");

        Instant draftCutoff  = Instant.now().minus(24, ChronoUnit.HOURS);
        Instant readyCutoff  = Instant.now().minus(48, ChronoUnit.HOURS);
        Instant labelCutoff  = Instant.now().minus(72, ChronoUnit.HOURS);

        List<Shipment> staleDrafts = shipmentRepository.findByStatusAndCreatedAtBefore(ShipmentStatus.DRAFT, draftCutoff);
        if (!staleDrafts.isEmpty()) {
            log.warn("StaleShipmentDetectionJob: {} DRAFT shipments older than 24h detected", staleDrafts.size());
            for (Shipment s : staleDrafts) {
                log.warn("Stale DRAFT shipment: id={} shipmentNumber={} createdAt={}",
                        s.getPublicId(), s.getShipmentNumber(), s.getCreatedAt());
            }
        }

        List<Shipment> staleReady = shipmentRepository.findByStatusAndCreatedAtBefore(ShipmentStatus.READY, readyCutoff);
        if (!staleReady.isEmpty()) {
            log.warn("StaleShipmentDetectionJob: {} READY shipments older than 48h detected", staleReady.size());
            for (Shipment s : staleReady) {
                log.warn("Stale READY shipment: id={} shipmentNumber={} createdAt={}",
                        s.getPublicId(), s.getShipmentNumber(), s.getCreatedAt());
            }
        }

        List<Shipment> staleLabel = shipmentRepository.findByStatusAndCreatedAtBefore(ShipmentStatus.LABEL_CREATED, labelCutoff);
        if (!staleLabel.isEmpty()) {
            log.warn("StaleShipmentDetectionJob: {} LABEL_CREATED shipments older than 72h detected", staleLabel.size());
            for (Shipment s : staleLabel) {
                log.warn("Stale LABEL_CREATED shipment: id={} shipmentNumber={} trackingNumber={}",
                        s.getPublicId(), s.getShipmentNumber(), s.getTrackingNumber());
            }
        }

        log.info("StaleShipmentDetectionJob: scan complete — stale_drafts={} stale_ready={} stale_labels={}",
                staleDrafts.size(), staleReady.size(), staleLabel.size());
    }
}

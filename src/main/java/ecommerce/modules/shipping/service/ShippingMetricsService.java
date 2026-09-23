package ecommerce.modules.shipping.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ShippingMetricsService {

    private final Counter shipmentsCreated;
    private final Counter shipmentsDelivered;
    private final Counter shipmentsFailed;
    private final Counter shipmentsCancelled;
    private final Counter webhooksReceived;
    private final Counter webhooksRetried;
    private final Counter exceptionsCreated;
    private final Counter exceptionsResolved;

    public ShippingMetricsService(MeterRegistry registry) {
        this.shipmentsCreated   = Counter.builder("shipping.shipments.created")
                .description("Total shipments created").register(registry);
        this.shipmentsDelivered = Counter.builder("shipping.shipments.delivered")
                .description("Total shipments delivered").register(registry);
        this.shipmentsFailed    = Counter.builder("shipping.shipments.failed")
                .description("Total shipments failed or returned").register(registry);
        this.shipmentsCancelled = Counter.builder("shipping.shipments.cancelled")
                .description("Total shipments cancelled").register(registry);
        this.webhooksReceived   = Counter.builder("shipping.webhooks.received")
                .description("Total carrier webhooks ingested").register(registry);
        this.webhooksRetried    = Counter.builder("shipping.webhooks.retried")
                .description("Total carrier webhooks retried by job").register(registry);
        this.exceptionsCreated  = Counter.builder("shipping.exceptions.created")
                .description("Total shipment exceptions created").register(registry);
        this.exceptionsResolved = Counter.builder("shipping.exceptions.resolved")
                .description("Total shipment exceptions resolved").register(registry);
    }

    public void recordShipmentCreated()    { shipmentsCreated.increment(); }
    public void recordShipmentDelivered()  { shipmentsDelivered.increment(); }
    public void recordShipmentFailed()     { shipmentsFailed.increment(); }
    public void recordShipmentCancelled()  { shipmentsCancelled.increment(); }
    public void recordWebhookReceived()    { webhooksReceived.increment(); }
    public void recordWebhookRetried()     { webhooksRetried.increment(); }
    public void recordExceptionCreated()   { exceptionsCreated.increment(); }
    public void recordExceptionResolved()  { exceptionsResolved.increment(); }

    @Scheduled(fixedRate = 300_000)
    public void logShippingMetrics() {
        log.info("Shipping metrics snapshot: shipments_created={}, delivered={}, failed={}, cancelled={}, " +
                        "webhooks_received={}, webhooks_retried={}, exceptions_created={}, exceptions_resolved={}",
                (long) shipmentsCreated.count(),
                (long) shipmentsDelivered.count(),
                (long) shipmentsFailed.count(),
                (long) shipmentsCancelled.count(),
                (long) webhooksReceived.count(),
                (long) webhooksRetried.count(),
                (long) exceptionsCreated.count(),
                (long) exceptionsResolved.count());
    }
}

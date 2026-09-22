package ecommerce.modules.shipping.service;

public interface ShippingWebhookService {
    boolean ingest(String provider, String eventId, String eventType, String rawPayload);
}

package ecommerce.modules.shipping.service.impl;

import ecommerce.modules.shipping.entity.ShippingWebhookEvent;
import ecommerce.modules.shipping.repository.ShippingWebhookEventRepository;
import ecommerce.modules.shipping.service.ShippingWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingWebhookServiceImpl implements ShippingWebhookService {

    private final ShippingWebhookEventRepository webhookEventRepository;

    @Override
    @Transactional
    public boolean ingest(String provider, String eventId, String eventType, String rawPayload) {
        if (eventId != null && webhookEventRepository.existsByProviderAndProviderEventId(provider, eventId)) {
            log.debug("Duplicate shipping webhook ignored: provider={} eventId={}", provider, eventId);
            return false;
        }

        ShippingWebhookEvent event = ShippingWebhookEvent.builder()
                .provider(provider)
                .providerEventId(eventId != null ? eventId : UUID.randomUUID().toString())
                .eventType(eventType)
                .payload(rawPayload)
                .build();
        webhookEventRepository.save(event);
        log.info("Ingested shipping webhook: provider={} eventType={}", provider, eventType);
        return true;
    }
}

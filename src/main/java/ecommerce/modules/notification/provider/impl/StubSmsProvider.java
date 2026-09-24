package ecommerce.modules.notification.provider.impl;

import ecommerce.modules.notification.provider.SmsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * No-op SMS provider used when no real SMS vendor is configured.
 * Logs the outbound message and returns a synthetic message ID.
 * Replace by wiring a real implementation (Twilio, Hubtel, etc.) and
 * qualifying with @Primary or @ConditionalOnProperty.
 */
@Slf4j
@Component
@ConditionalOnMissingBean(name = "realSmsProvider")
public class StubSmsProvider implements SmsProvider {

    @Override
    public String send(String recipientPhone, String body, String idempotencyKey) {
        String messageId = "stub-sms-" + UUID.randomUUID();
        log.info("[SMS-STUB] Would send to={} idempotencyKey={} messageId={} body={}",
                recipientPhone, idempotencyKey, messageId, body);
        return messageId;
    }

    @Override
    public boolean isRetryable(String failureCode) {
        return false;
    }

    @Override
    public String providerName() {
        return "STUB_SMS";
    }
}

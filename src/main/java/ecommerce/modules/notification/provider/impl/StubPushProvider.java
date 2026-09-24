package ecommerce.modules.notification.provider.impl;

import ecommerce.modules.notification.provider.PushProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * No-op push provider used when no real push vendor is configured.
 * Logs the outbound push and returns a synthetic message ID.
 * Replace by wiring a real FCM/APNs implementation qualified with @Primary
 * or @ConditionalOnProperty.
 */
@Slf4j
@Component
@ConditionalOnMissingBean(name = "realPushProvider")
public class StubPushProvider implements PushProvider {

    @Override
    public String send(String deviceToken, String title, String body, Map<String, String> data) {
        String messageId = "stub-push-" + UUID.randomUUID();
        log.info("[PUSH-STUB] Would send to deviceToken={}... title='{}' messageId={}",
                deviceToken.length() > 12 ? deviceToken.substring(0, 12) : deviceToken,
                title, messageId);
        return messageId;
    }

    @Override
    public boolean isRetryable(String failureCode) {
        return false;
    }

    @Override
    public boolean isInvalidToken(String failureCode) {
        return false;
    }

    @Override
    public String providerName() {
        return "STUB_PUSH";
    }
}

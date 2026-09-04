package ecommerce.modules.notification.provider.impl;

import ecommerce.modules.notification.dto.EmailRequest;
import ecommerce.modules.notification.exceptions.EmailDispatchException;
import ecommerce.modules.notification.provider.EmailProvider;
import io.mailtrap.client.MailtrapClient;
import io.mailtrap.config.MailtrapConfig;
import io.mailtrap.factory.MailtrapClientFactory;
import io.mailtrap.model.request.emails.Address;
import io.mailtrap.model.request.emails.MailtrapMail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "fynza.notification.email.provider", havingValue = "mailtrap")
public class MailtrapSandboxEmailProvider implements EmailProvider {

    @Value("${fynza.mail.mailtrap.token:}")
    private String token;

    @Value("${fynza.mail.mailtrap.inbox-id:0}")
    private Long inboxId;

    @Value("${fynza.notification.email.from}")
    private String fromAddress;

    @Value("${fynza.notification.email.from-name:Fynza}")
    private String fromName;

    @Override
    public String providerName() { return "MAILTRAP_SANDBOX"; }

    @Override
    public String send(EmailRequest request) {
        try {
            MailtrapConfig config = new MailtrapConfig.Builder()
                    .sandbox(true)
                    .inboxId(inboxId)
                    .token(token)
                    .build();

            MailtrapClient client = MailtrapClientFactory.createMailtrapClient(config);

            MailtrapMail.MailtrapMailBuilder builder = MailtrapMail.builder()
                    .from(new Address(fromAddress, fromName))
                    .to(request.getTo().stream().map(e -> new Address(e, e)).toList())
                    .subject(request.getSubject())
                    .category("FYNZA-" + request.getTags().getOrDefault("notificationType", "GENERAL"));

            if (request.getHtmlBody() != null) builder.html(request.getHtmlBody()).text(request.getTextBody());
            else builder.text(request.getTextBody());

            var response = client.send(builder.build());
            String messageId = "mailtrap-" + System.currentTimeMillis();
            log.info("[Mailtrap] Intercepted to={} subject='{}' response={}", request.getTo(), request.getSubject(), response);
            return messageId;
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            log.error("[Mailtrap] Send failed: {}", msg);
            throw new EmailDispatchException(providerName(), msg, e, true);
        }
    }
}

package ecommerce.modules.notification.provider.impl;

import ecommerce.modules.notification.dto.EmailRequest;
import ecommerce.modules.notification.exceptions.EmailDispatchException;
import ecommerce.modules.notification.provider.EmailProvider;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "fynza.notification.email.provider", havingValue = "gmail", matchIfMissing = true)
public class GmailSmtpEmailProvider implements EmailProvider {

    private final JavaMailSender mailSender;

    @Value("${fynza.notification.email.from}")
    private String fromAddress;

    @Value("${fynza.notification.email.from-name:Fynza}")
    private String fromName;

    @Override
    public String providerName() { return "GMAIL_SMTP"; }

    @Override
    public String send(EmailRequest request) {
        try {
            mailSender.send(buildMessage(request));
            String localId = "gmail-" + UUID.randomUUID();
            log.info("[Gmail SMTP] Sent to={} subject='{}' id={}", request.getTo(), request.getSubject(), localId);
            return localId;
        } catch (MailAuthenticationException e) {
            log.error("[Gmail SMTP] Auth failed: {}", safeMsg(e));
            throw new EmailDispatchException(providerName(), "Gmail auth failed: " + safeMsg(e), e, false);
        } catch (MailSendException e) {
            boolean retryable = isRetryable(e);
            log.error("[Gmail SMTP] Send failed retryable={} error={}", retryable, safeMsg(e));
            throw new EmailDispatchException(providerName(), safeMsg(e), e, retryable);
        } catch (MessagingException e) {
            log.error("[Gmail SMTP] Message build failed: {}", safeMsg(e));
            throw new EmailDispatchException(providerName(), "Message build failed: " + safeMsg(e), e, false);
        } catch (Exception e) {
            log.error("[Gmail SMTP] Unexpected error: {}", safeMsg(e));
            throw new EmailDispatchException(providerName(), safeMsg(e), e, true);
        }
    }

    private MimeMessage buildMessage(EmailRequest request) throws MessagingException, UnsupportedEncodingException {
        boolean multipart = request.getHtmlBody() != null;
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, multipart, "UTF-8");
        helper.setFrom(fromAddress, fromName);
        helper.setTo(request.getTo().toArray(String[]::new));
        if (request.getCc() != null && !request.getCc().isEmpty()) helper.setCc(request.getCc().toArray(String[]::new));
        if (request.getBcc() != null && !request.getBcc().isEmpty()) helper.setBcc(request.getBcc().toArray(String[]::new));
        if (request.getReplyTo() != null) helper.setReplyTo(request.getReplyTo());
        helper.setSubject(request.getSubject());
        if (multipart) helper.setText(request.getTextBody(), request.getHtmlBody());
        else helper.setText(request.getTextBody(), false);
        return message;
    }

    private boolean isRetryable(MailSendException e) {
        String msg = safeMsg(e).toLowerCase();
        return !msg.contains("invalid address") && !msg.contains("no such user")
            && !msg.contains("user unknown") && !msg.contains("address rejected");
    }

    private static String safeMsg(Exception e) {
        String raw = e.getMessage();
        return raw != null ? raw : "";
    }
}

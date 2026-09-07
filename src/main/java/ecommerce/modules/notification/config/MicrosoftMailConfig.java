package ecommerce.modules.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/** Microsoft Office 365 SMTP JavaMailSender — active when fynza.notification.email.provider=microsoft. */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "fynza.notification.email.provider", havingValue = "microsoft")
public class MicrosoftMailConfig {

    @Value("${fynza.mail.microsoft.host:smtp.office365.com}")
    private String host;

    @Value("${fynza.mail.microsoft.port:587}")
    private int port;

    @Value("${fynza.mail.microsoft.username}")
    private String username;

    @Value("${fynza.mail.microsoft.password}")
    private String password;

    @Value("${fynza.mail.microsoft.encryption:tls}")
    private String encryption;

    @Bean
    public JavaMailSender javaMailSender() {
        log.info("[MailConfig] Microsoft SMTP — host={} port={} encryption={}", host, port, encryption);
        return MailSenderFactory.create(host, port, username, password, encryption);
    }
}

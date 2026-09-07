package ecommerce.modules.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/** Gmail SMTP JavaMailSender — active when fynza.notification.email.provider=gmail (default). */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "fynza.notification.email.provider", havingValue = "gmail", matchIfMissing = true)
public class GmailMailConfig {

    @Value("${fynza.mail.gmail.host:smtp.gmail.com}")
    private String host;

    @Value("${fynza.mail.gmail.port:587}")
    private int port;

    @Value("${fynza.mail.gmail.username:}")
    private String username;

    @Value("${fynza.mail.gmail.password:}")
    private String password;

    @Bean
    @ConditionalOnMissingBean(JavaMailSender.class)
    public JavaMailSender javaMailSender() {
        log.info("[MailConfig] Gmail SMTP — host={} port={}", host, port);
        return MailSenderFactory.create(host, port, username, password, "tls");
    }
}

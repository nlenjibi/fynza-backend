package ecommerce.modules.notification.config;

import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

final class MailSenderFactory {

    private MailSenderFactory() {}

    static JavaMailSenderImpl create(String host, int port, String username, String password, String encryption) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        applyEncryption(props, encryption);
        return sender;
    }

    private static void applyEncryption(Properties props, String encryption) {
        if ("tls".equalsIgnoreCase(encryption)) {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        } else if ("ssl".equalsIgnoreCase(encryption)) {
            props.put("mail.smtp.ssl.enable", "true");
        }
    }
}

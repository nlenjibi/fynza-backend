package ecommerce.modules.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "fynza.notification.slack")
public class SlackProperties {

    private String botToken = "";
    private String apiBaseUrl = "https://slack.com/api";
}

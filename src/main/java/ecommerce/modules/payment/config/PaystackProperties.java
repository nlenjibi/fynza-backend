package ecommerce.modules.payment.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Paystack payment gateway configuration properties.
 */
@Configuration
@ConfigurationProperties(prefix = "paystack")
@Getter
@Setter
@Validated
public class PaystackProperties {

    @NotBlank(message = "Paystack secret key is required")
    private String secretKey;

    @NotBlank(message = "Paystack public key is required")
    private String publicKey;

    @NotBlank(message = "Paystack base URL is required")
    private String baseUrl = "https://api.paystack.co";

    private String webhookSecret;

    private int connectTimeout = 30000;
    private int readTimeout = 30000;
}
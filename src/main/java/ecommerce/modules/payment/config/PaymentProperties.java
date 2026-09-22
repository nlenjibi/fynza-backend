package ecommerce.modules.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "fynza.payment")
@Getter
@Setter
public class PaymentProperties {

    private String provider = "paystack";

    private PaystackConfig paystack       = new PaystackConfig();
    private StripeConfig   stripe         = new StripeConfig();
    private PaypalConfig   paypal         = new PaypalConfig();
    private FlutterwaveConfig flutterwave = new FlutterwaveConfig();

    @Getter @Setter
    public static class PaystackConfig {
        private String secretKey;
        private String publicKey;
        private String baseUrl         = "https://api.paystack.co";
        private String webhookSecret;
        private int    connectTimeout  = 30_000;
        private int    readTimeout     = 30_000;
    }

    @Getter @Setter
    public static class StripeConfig {
        private String secretKey;
        private String publicKey;
        private String webhookSecret;
        private String baseUrl        = "https://api.stripe.com";
        private int    connectTimeout = 30_000;
        private int    readTimeout    = 30_000;
    }

    @Getter @Setter
    public static class PaypalConfig {
        private String  clientId;
        private String  clientSecret;
        private String  webhookId;
        private boolean sandbox        = false;
        private String  baseUrl        = "https://api-m.paypal.com";
        private int     connectTimeout = 30_000;
        private int     readTimeout    = 30_000;
    }

    @Getter @Setter
    public static class FlutterwaveConfig {
        private String secretKey;
        private String publicKey;
        private String encryptionKey;
        private String webhookSecret;
        private String baseUrl        = "https://api.flutterwave.com/v3";
        private int    connectTimeout = 30_000;
        private int    readTimeout    = 30_000;
    }
}

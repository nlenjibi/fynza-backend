package ecommerce.modules.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Paystack payment initialization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaystackInitializeResponse {

    private boolean status;
    private String message;

    @JsonProperty("data")
    private PaymentData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentData {
        private String authorizationUrl;
        private String accessCode;
        private String reference;
        private String url;
    }
}
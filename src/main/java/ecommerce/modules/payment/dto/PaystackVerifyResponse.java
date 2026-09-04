package ecommerce.modules.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for Paystack transaction verification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaystackVerifyResponse {

    private boolean status;
    private String message;

    @JsonProperty("data")
    private TransactionData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionData {
        private long id;
        private String reference;
        private String transactionId;
        private String status;
        private String channel;
        private String currency;
        private BigDecimal amount;
        private BigDecimal fees;
        private CustomerData customer;
        private String createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerData {
        private String email;
        private String customerCode;
        private String phone;
    }
}
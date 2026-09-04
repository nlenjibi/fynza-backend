package ecommerce.modules.payment.async;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResult {

    private UUID transactionId;
    private PaymentStatus status;
    private String message;
    private String gateway;
    private BigDecimal amount;
    private String authorizationCode;

    public enum PaymentStatus {
        PROCESSING,
        AUTHORIZED,
        CAPTURED,
        FAILED,
        DECLINED,
        TIMEOUT
    }

    public static PaymentResult processing(UUID transactionId, BigDecimal amount) {
        return PaymentResult.builder()
                .transactionId(transactionId)
                .status(PaymentStatus.PROCESSING)
                .message("Payment is being processed")
                .amount(amount)
                .build();
    }

    public static PaymentResult success(UUID transactionId, String gateway, BigDecimal amount, String authCode) {
        return PaymentResult.builder()
                .transactionId(transactionId)
                .status(PaymentStatus.CAPTURED)
                .message("Payment successful")
                .gateway(gateway)
                .amount(amount)
                .authorizationCode(authCode)
                .build();
    }

    public static PaymentResult failed(UUID transactionId, String message) {
        return PaymentResult.builder()
                .transactionId(transactionId)
                .status(PaymentStatus.FAILED)
                .message(message)
                .build();
    }
}

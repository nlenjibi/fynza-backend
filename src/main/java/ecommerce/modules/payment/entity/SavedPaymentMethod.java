package ecommerce.modules.payment.entity;

import ecommerce.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "saved_payment_methods", indexes = {
    @Index(name = "idx_payment_method_user", columnList = "user_id"),
    @Index(name = "idx_payment_method_default", columnList = "is_default")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SavedPaymentMethod extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "method_type", nullable = false, length = 30)
    private PaymentMethodType methodType;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "last_four", length = 4)
    private String lastFour;

    @Column(name = "card_brand", length = 20)
    private String cardBrand;

    @Column(name = "expiry_month")
    private Integer expiryMonth;

    @Column(name = "expiry_year")
    private Integer expiryYear;

    @Column(name = "card_holder_name", length = 100)
    private String cardHolderName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "account_number", length = 20)
    private String accountNumber;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "account_holder_name", length = 100)
    private String accountHolderName;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "last_used_at")
    private LocalDate lastUsedAt;

    @Column(name = "paystack_customer_id", length = 100)
    private String paystackCustomerId;

    @Column(name = "paystack_authorization_code", length = 100)
    private String paystackAuthorizationCode;

    public enum PaymentMethodType {
        CARD,
        MOBILE_MONEY,
        BANK_TRANSFER,
        WALLET
    }

    public boolean isExpired() {
        if (methodType != PaymentMethodType.CARD) {
            return false;
        }
        LocalDate now = LocalDate.now();
        return expiryYear < now.getYear() || 
               (expiryYear == now.getYear() && expiryMonth < now.getMonthValue());
    }
}

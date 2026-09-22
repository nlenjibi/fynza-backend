package ecommerce.modules.payment.provider;

public enum PaymentProviderType {
    PAYSTACK,
    STRIPE,
    PAYPAL,
    FLUTTERWAVE;

    public static PaymentProviderType fromString(String value) {
        return valueOf(value.toUpperCase());
    }
}

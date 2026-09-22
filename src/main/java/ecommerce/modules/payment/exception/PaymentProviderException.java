package ecommerce.modules.payment.exception;

public class PaymentProviderException extends RuntimeException {

    private final String provider;

    public PaymentProviderException(String provider, String message) {
        super("[" + provider + "] " + message);
        this.provider = provider;
    }

    public PaymentProviderException(String provider, String message, Throwable cause) {
        super("[" + provider + "] " + message, cause);
        this.provider = provider;
    }

    public String getProvider() {
        return provider;
    }
}

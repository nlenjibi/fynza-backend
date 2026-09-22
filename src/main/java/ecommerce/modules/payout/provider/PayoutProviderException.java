package ecommerce.modules.payout.provider;

public class PayoutProviderException extends RuntimeException {

    private final String provider;

    public PayoutProviderException(String provider, String message) {
        super("[" + provider + "] " + message);
        this.provider = provider;
    }

    public PayoutProviderException(String provider, String message, Throwable cause) {
        super("[" + provider + "] " + message, cause);
        this.provider = provider;
    }

    public String getProvider() {
        return provider;
    }
}

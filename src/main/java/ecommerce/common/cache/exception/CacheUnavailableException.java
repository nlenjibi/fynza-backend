package ecommerce.common.cache.exception;

public class CacheUnavailableException extends CacheException {

    public CacheUnavailableException(String message) {
        super(message);
    }

    public CacheUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

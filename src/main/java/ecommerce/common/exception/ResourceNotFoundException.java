package ecommerce.common.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public static ResourceNotFoundException forResource(String resourceName, Long id) {
        return new ResourceNotFoundException(resourceName + " not found with id: " + id);
    }

    public static ResourceNotFoundException forResource(String resourceName, UUID id) {
        return new ResourceNotFoundException(resourceName + " not found with id: " + id);
    }

}

package ecommerce.common.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ResourceNotFoundException extends FynzaException {

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    protected ResourceNotFoundException(String message, String code) {
        super(message, HttpStatus.NOT_FOUND, code);
    }

    public static ResourceNotFoundException forResource(String resourceName, Long id) {
        return new ResourceNotFoundException(resourceName + " not found with id: " + id);
    }

    public static ResourceNotFoundException forResource(String resourceName, UUID id) {
        return new ResourceNotFoundException(resourceName + " not found with id: " + id);
    }

    public static ResourceNotFoundException forResource(String resourceName, String identifier) {
        return new ResourceNotFoundException(resourceName + " not found: " + identifier);
    }
}

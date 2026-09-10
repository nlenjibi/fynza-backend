package ecommerce.modules.category.exception;

import ecommerce.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class CategoryNotFoundException extends ResourceNotFoundException {

    public CategoryNotFoundException(UUID publicId) {
        super("Category not found: " + publicId, "CATEGORY_NOT_FOUND");
    }

    public CategoryNotFoundException(String message) {
        super(message);
    }
}

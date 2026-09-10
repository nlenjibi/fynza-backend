package ecommerce.modules.category.exception;

import ecommerce.common.exception.DuplicateResourceException;

public class CategoryAlreadyExistsException extends DuplicateResourceException {

    public CategoryAlreadyExistsException(String slug) {
        super("Category with slug already exists: " + slug, "CATEGORY_ALREADY_EXISTS");
    }
}

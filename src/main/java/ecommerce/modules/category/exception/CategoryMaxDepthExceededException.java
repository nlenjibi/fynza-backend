package ecommerce.modules.category.exception;

import ecommerce.common.exception.BadRequestException;

public class CategoryMaxDepthExceededException extends BadRequestException {

    public CategoryMaxDepthExceededException(int maxDepth) {
        super("Category hierarchy cannot exceed " + maxDepth + " levels");
    }
}

package ecommerce.modules.category.validation;

import ecommerce.modules.category.entity.Category;
import ecommerce.modules.category.exception.CategoryCircularHierarchyException;
import ecommerce.modules.category.exception.CategoryMaxDepthExceededException;
import ecommerce.modules.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CategoryHierarchyValidator {

    private static final int DEFAULT_MAX_DEPTH = 5;

    @Value("${category.max-depth:" + DEFAULT_MAX_DEPTH + "}")
    private int maxDepth;

    private final CategoryRepository categoryRepository;

    public void validateMove(Category category, Category newParent) {
        if (newParent == null) return;
        validateNoCircularDependency(category, newParent);
        validateDepth(newParent);
    }

    public void validateDepth(Category parent) {
        if (parent == null) return;
        int depth = computeDepth(parent);
        if (depth >= maxDepth) {
            throw new CategoryMaxDepthExceededException(maxDepth);
        }
    }

    private void validateNoCircularDependency(Category category, Category newParent) {
        Category cursor = newParent;
        while (cursor != null) {
            if (cursor.getId().equals(category.getId())) {
                throw new CategoryCircularHierarchyException(category.getName());
            }
            cursor = cursor.getParentCategory();
            if (cursor != null && cursor.getParentCategory() == null && cursor.getId() != null) {
                cursor = categoryRepository.findById(cursor.getId())
                        .map(Category::getParentCategory)
                        .orElse(null);
            }
        }
    }

    private int computeDepth(Category category) {
        int depth = 0;
        Category cursor = category;
        while (cursor.getParentCategory() != null) {
            depth++;
            cursor = cursor.getParentCategory();
            if (depth > maxDepth) break;
        }
        return depth;
    }
}

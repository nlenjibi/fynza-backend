package ecommerce.modules.category.repository;

import ecommerce.modules.category.entity.CategorySuggestion;
import ecommerce.modules.category.enums.CategorySuggestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategorySuggestionRepository extends JpaRepository<CategorySuggestion, Long> {

    Optional<CategorySuggestion> findByPublicId(UUID publicId);

    Page<CategorySuggestion> findByStatus(CategorySuggestionStatus status, Pageable pageable);

    Page<CategorySuggestion> findByRequestedBy(UUID requestedBy, Pageable pageable);
}

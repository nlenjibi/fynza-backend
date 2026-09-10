package ecommerce.modules.category.repository;

import ecommerce.modules.category.entity.CategorySummaryView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategorySummaryViewRepository
        extends JpaRepository<CategorySummaryView, Long>, JpaSpecificationExecutor<CategorySummaryView> {

    Optional<CategorySummaryView> findByPublicId(UUID publicId);

    Optional<CategorySummaryView> findBySlug(String slug);

    List<CategorySummaryView> findByParentIdIsNullAndIsActiveTrueOrderBySortOrderAsc();

    List<CategorySummaryView> findByParentIdOrderBySortOrderAsc(Long parentId);

    List<CategorySummaryView> findByTaxonomyIdAndParentIdIsNullOrderBySortOrderAsc(Long taxonomyId);
}

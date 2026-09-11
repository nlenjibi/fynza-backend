package ecommerce.modules.category.repository;

import ecommerce.modules.category.entity.Category;
import ecommerce.modules.category.enums.CategoryStatus;
import ecommerce.modules.category.enums.CategoryVisibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {

    Optional<Category> findByPublicId(UUID publicId);

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndTaxonomyId(String slug, Long taxonomyId);

    List<Category> findByParentCategoryIsNullAndStatusAndVisibilityOrderBySortOrderAsc(
            CategoryStatus status, CategoryVisibility visibility);

    List<Category> findByParentCategoryIsNullOrderBySortOrderAsc();

    List<Category> findByParentCategory_IdOrderBySortOrderAsc(Long parentId);

    List<Category> findByTaxonomyIdAndParentCategoryIsNullOrderBySortOrderAsc(Long taxonomyId);

    long countByParentCategory_Id(Long parentId);

    @Query("SELECT COUNT(c) FROM Category c WHERE c.isActive = true")
    long countActiveCategories();

    @Query("SELECT COUNT(c) FROM Category c WHERE c.parentCategory IS NULL")
    long countRootCategories();

    @Query("SELECT c FROM Category c WHERE c.isActive = :isActive")
    List<Category> findByIsActive(@Param("isActive") Boolean isActive);

    @Query("SELECT c FROM Category c WHERE c.parentCategory IS NULL AND c.status = 'ACTIVE' ORDER BY c.sortOrder ASC")
    List<Category> findActiveRootCategories();
}

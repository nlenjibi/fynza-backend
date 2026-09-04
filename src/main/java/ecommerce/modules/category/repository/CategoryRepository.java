package ecommerce.modules.category.repository;

import ecommerce.common.base.BaseRepository;
import ecommerce.modules.category.entity.Category;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends BaseRepository<Category, UUID> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Category> findByParentCategoryId(UUID parentCategoryId);

    @Query("SELECT c FROM Category c WHERE c.parentCategory IS NULL")
    List<Category> findRootCategories();

    @Query("SELECT c FROM Category c WHERE c.isActive = :isActive")
    List<Category> findByIsActive(@Param("isActive") Boolean isActive);

    @Query("SELECT COUNT(c) FROM Category c WHERE c.isActive = true")
    long countActiveCategories();

    @Query("SELECT COUNT(c) FROM Category c WHERE c.parentCategory IS NOT NULL")
    long countSubcategories();

    @Query("SELECT COUNT(c) FROM Category c WHERE c.parentCategory IS NULL")
    long countParentCategories();
}

package ecommerce.modules.category.repository;

import ecommerce.modules.category.entity.AttributeDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttributeDefinitionRepository extends JpaRepository<AttributeDefinition, Long> {

    Optional<AttributeDefinition> findByPublicId(UUID publicId);

    List<AttributeDefinition> findByCategoryIdAndIsActiveTrueOrderBySortOrderAsc(Long categoryId);

    boolean existsByCategoryIdAndCode(Long categoryId, String code);
}

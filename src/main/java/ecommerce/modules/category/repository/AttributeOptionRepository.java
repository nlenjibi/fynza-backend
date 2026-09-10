package ecommerce.modules.category.repository;

import ecommerce.modules.category.entity.AttributeOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttributeOptionRepository extends JpaRepository<AttributeOption, Long> {

    Optional<AttributeOption> findByPublicId(UUID publicId);

    List<AttributeOption> findByAttributeDefinitionIdAndIsActiveTrueOrderBySortOrderAsc(Long attributeDefinitionId);
}

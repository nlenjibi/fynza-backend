package ecommerce.modules.category.repository;

import ecommerce.modules.category.entity.Taxonomy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaxonomyRepository extends JpaRepository<Taxonomy, Long> {

    Optional<Taxonomy> findByCode(String code);

    Optional<Taxonomy> findByPublicId(UUID publicId);
}

package ecommerce.modules.search.repository;

import ecommerce.modules.search.entity.SearchSynonym;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SearchSynonymRepository extends JpaRepository<SearchSynonym, UUID> {

    Optional<SearchSynonym> findByTermIgnoreCase(String term);

    List<SearchSynonym> findByStatusAndIsActiveTrue(String status);
}

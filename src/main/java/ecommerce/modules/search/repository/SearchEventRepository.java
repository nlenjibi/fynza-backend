package ecommerce.modules.search.repository;

import ecommerce.modules.search.entity.SearchEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SearchEventRepository extends JpaRepository<SearchEvent, UUID> {
}

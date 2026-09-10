package ecommerce.modules.store.repository;

import ecommerce.modules.store.entity.StoreSlugHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoreSlugHistoryRepository extends JpaRepository<StoreSlugHistory, Long> {

    Optional<StoreSlugHistory> findTopBySlugOrderByCreatedAtDesc(String slug);

    boolean existsBySlug(String slug);
}

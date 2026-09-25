package ecommerce.modules.search.repository;

import ecommerce.modules.search.entity.SearchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, UUID> {

    Page<SearchHistory> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM SearchHistory s WHERE s.userId = :userId")
    void deleteByUserId(UUID userId);
}

package ecommerce.modules.tag.repository;

import ecommerce.common.base.BaseRepository;
import ecommerce.modules.tag.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagRepository extends BaseRepository<Tag, UUID> {

    Optional<Tag> findByName(String name);

    Page<Tag> findByIsActiveTrue(Pageable pageable);

    Page<Tag> findByIsFeaturedTrue(Pageable pageable);

    @Query("SELECT t FROM Tag t WHERE t.isActive = true ORDER BY t.usageCount DESC")
    List<Tag> findMostUsedTags(Pageable pageable);

    @Modifying
    @Query("UPDATE Tag t SET t.usageCount = t.usageCount + 1 WHERE t.id = :tagId")
    void incrementUsageCount(@Param("tagId") UUID tagId);

    @Modifying
    @Query("UPDATE Tag t SET t.usageCount = t.usageCount - 1 WHERE t.id = :tagId AND t.usageCount > 0")
    void decrementUsageCount(@Param("tagId") UUID tagId);

    boolean existsByName(String name);
}

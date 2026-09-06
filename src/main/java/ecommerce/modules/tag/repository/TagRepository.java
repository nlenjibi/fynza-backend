package ecommerce.modules.tag.repository;

import ecommerce.modules.tag.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByPublicId(UUID publicId);

    Optional<Tag> findByName(String name);

    Page<Tag> findByIsActiveTrue(Pageable pageable);

    Page<Tag> findByIsFeaturedTrue(Pageable pageable);

    @Query("SELECT t FROM Tag t WHERE t.isActive = true ORDER BY t.usageCount DESC")
    List<Tag> findMostUsedTags(Pageable pageable);

    @Modifying
    @Query("UPDATE Tag t SET t.usageCount = t.usageCount + 1 WHERE t.id = :tagId")
    void incrementUsageCount(@Param("tagId") Long tagId);

    @Modifying
    @Query("UPDATE Tag t SET t.usageCount = t.usageCount - 1 WHERE t.id = :tagId AND t.usageCount > 0")
    void decrementUsageCount(@Param("tagId") Long tagId);

    boolean existsByName(String name);
}

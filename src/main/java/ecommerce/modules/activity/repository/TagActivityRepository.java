package ecommerce.modules.activity.repository;

import ecommerce.modules.activity.entity.TagActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TagActivityRepository extends JpaRepository<TagActivity, UUID> {

    Page<TagActivity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<TagActivity> findByTagIdOrderByCreatedAtDesc(UUID tagId, Pageable pageable);

    Page<TagActivity> findByProductIdOrderByCreatedAtDesc(UUID productId, Pageable pageable);
}

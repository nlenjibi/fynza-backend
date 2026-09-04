package ecommerce.common.base;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Base repository for all Fynza entities.
 * PK is always Long (BIGINT). API-layer callers resolve publicId → Long id once
 * at the boundary; all intra-service FK relationships use Long.
 */
@NoRepositoryBean
public interface BaseRepository<T extends BaseEntity> extends JpaRepository<T, Long>, QuerydslPredicateExecutor<T> {

    Page<T> findByIsActiveTrue(Pageable pageable);

    Optional<T> findByIdAndIsActiveTrue(Long id);

    Optional<T> findByPublicId(UUID publicId);

    Optional<T> findByPublicIdAndIsActiveTrue(UUID publicId);

    Page<T> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    Page<T> findByUpdatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    boolean existsByIdAndIsActiveTrue(Long id);

    boolean existsByPublicIdAndIsActiveTrue(UUID publicId);

    long countByIsActiveTrue();
}

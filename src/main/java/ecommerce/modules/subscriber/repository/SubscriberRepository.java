package ecommerce.modules.subscriber.repository;

import ecommerce.common.base.BaseRepository;
import ecommerce.modules.subscriber.entity.Subscriber;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriberRepository extends BaseRepository<Subscriber, UUID> {

    Optional<Subscriber> findByEmail(String email);

    Optional<Subscriber> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    long countByStatus(Subscriber.SubscriberStatus status);

    @Query("SELECT s FROM Subscriber s WHERE s.status = :status")
    Page<Subscriber> findByStatus(@Param("status") Subscriber.SubscriberStatus status, Pageable pageable);

    @Query("SELECT s FROM Subscriber s WHERE LOWER(s.email) LIKE LOWER(CONCAT('%', :email, '%'))")
    Page<Subscriber> searchByEmail(@Param("email") String email, Pageable pageable);

    @Query("SELECT s FROM Subscriber s WHERE s.status = :status AND LOWER(s.email) LIKE LOWER(CONCAT('%', :email, '%'))")
    Page<Subscriber> searchByEmailAndStatus(
            @Param("email") String email,
            @Param("status") Subscriber.SubscriberStatus status,
            Pageable pageable);
}

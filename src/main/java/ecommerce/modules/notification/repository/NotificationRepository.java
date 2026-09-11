package ecommerce.modules.notification.repository;

import ecommerce.modules.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.recipientId = :recipientId AND n.deletedAt IS NULL " +
           "ORDER BY n.createdAt DESC")
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(
            @Param("recipientId") UUID recipientId, Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipientId = :recipientId " +
           "AND n.read = false AND n.deletedAt IS NULL")
    long countUnreadByRecipientId(@Param("recipientId") UUID recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :now " +
           "WHERE n.publicId = :publicId AND n.recipientId = :recipientId AND n.read = false AND n.deletedAt IS NULL")
    int markAsRead(@Param("publicId") UUID publicId,
                   @Param("recipientId") UUID recipientId,
                   @Param("now") Instant now);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :now " +
           "WHERE n.recipientId = :recipientId AND n.read = false AND n.deletedAt IS NULL")
    void markAllReadForRecipient(@Param("recipientId") UUID recipientId, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE Notification n SET n.deletedAt = :now " +
           "WHERE n.publicId = :publicId AND n.recipientId = :recipientId AND n.deletedAt IS NULL")
    int softDelete(@Param("publicId") UUID publicId,
                   @Param("recipientId") UUID recipientId,
                   @Param("now") Instant now);

    @Modifying
    @Query("UPDATE Notification n SET n.deletedAt = :now " +
           "WHERE n.recipientId = :recipientId AND n.deletedAt IS NULL")
    void softDeleteAllForRecipient(@Param("recipientId") UUID recipientId, @Param("now") Instant now);

    @Query("SELECT n FROM Notification n WHERE n.publicId = :publicId AND n.recipientId = :recipientId AND n.deletedAt IS NULL")
    Optional<Notification> findByPublicIdAndRecipientId(@Param("publicId") UUID publicId,
                                                        @Param("recipientId") UUID recipientId);
}

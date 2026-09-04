package ecommerce.modules.message.repository;

import ecommerce.common.base.BaseRepository;
import ecommerce.common.enums.MessageStatus;
import ecommerce.common.enums.MessageType;
import ecommerce.modules.message.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends BaseRepository<Conversation, UUID> {

    Page<Conversation> findByParticipantIdOrderByCreatedAtDesc(UUID participantId, Pageable pageable);

    Page<Conversation> findByParticipantIdAndStatusOrderByCreatedAtDesc(UUID participantId, MessageStatus status, Pageable pageable);

    Page<Conversation> findByParticipantIdAndTypeOrderByCreatedAtDesc(UUID participantId, MessageType type, Pageable pageable);

    @Query("SELECT c FROM Conversation c WHERE c.participantId = :participantId AND " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:type IS NULL OR c.type = :type) AND " +
           "(:search IS NULL OR LOWER(c.subject) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY c.isPinned DESC, c.createdAt DESC")
    Page<Conversation> findByParticipantWithFilters(
            @Param("participantId") UUID participantId,
            @Param("status") MessageStatus status,
            @Param("type") MessageType type,
            @Param("search") String search,
            Pageable pageable);

    long countByParticipantIdAndUnreadCountGreaterThan(UUID participantId, int unreadCount);

    long countByParticipantIdAndStatus(UUID participantId, MessageStatus status);

    long countByStatus(MessageStatus status);

    Optional<Conversation> findByIdAndParticipantId(UUID id, UUID participantId);

    @Query("SELECT c FROM Conversation c WHERE " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:priority IS NULL OR c.priority = :priority) AND " +
           "(:search IS NULL OR LOWER(c.subject) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY c.isPinned DESC, c.priority DESC, c.createdAt DESC")
    Page<Conversation> findAllWithFilters(
            @Param("status") MessageStatus status,
            @Param("priority") String priority,
            @Param("search") String search,
            Pageable pageable);
}

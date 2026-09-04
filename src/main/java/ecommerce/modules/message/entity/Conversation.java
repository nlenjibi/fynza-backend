package ecommerce.modules.message.entity;

import ecommerce.common.base.BaseEntity;
import ecommerce.common.enums.ConversationCategory;
import ecommerce.common.enums.MessagePriority;
import ecommerce.common.enums.MessageStatus;
import ecommerce.common.enums.MessageType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "conversations", indexes = {
    @Index(name = "idx_conversation_status", columnList = "status"),
    @Index(name = "idx_conversation_priority", columnList = "priority"),
    @Index(name = "idx_conversation_type", columnList = "type"),
    @Index(name = "idx_conversation_participant", columnList = "participant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Conversation extends BaseEntity {

    @Column(name = "participant_id")
    private UUID participantId;

    @Column(name = "participant_type", length = 20)
    @Enumerated(EnumType.STRING)
    private MessageType participantType;

    @Column(name = "subject", length = 255)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 30)
    private ConversationCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 20)
    @Builder.Default
    private MessagePriority priority = MessagePriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private MessageStatus status = MessageStatus.OPEN;

    @Column(name = "type", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MessageType type = MessageType.CUSTOMER;

    @Column(name = "is_starred")
    @Builder.Default
    private Boolean isStarred = false;

    @Column(name = "is_pinned")
    @Builder.Default
    private Boolean isPinned = false;

    @Column(name = "last_message_preview", length = 255)
    private String lastMessagePreview;

    @Column(name = "unread_count")
    @Builder.Default
    private Integer unreadCount = 0;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "product_id")
    private UUID productId;
}

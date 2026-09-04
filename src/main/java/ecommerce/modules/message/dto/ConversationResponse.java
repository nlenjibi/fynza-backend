package ecommerce.modules.message.dto;

import ecommerce.common.enums.ConversationCategory;
import ecommerce.common.enums.MessagePriority;
import ecommerce.common.enums.MessageStatus;
import ecommerce.common.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {
    private UUID id;
    private String subject;
    private ConversationCategory category;
    private MessagePriority priority;
    private MessageStatus status;
    private MessageType type;
    private MessageType participantType;
    private Boolean isStarred;
    private Boolean isPinned;
    private String lastMessagePreview;
    private Integer unreadCount;
    private UUID orderId;
    private UUID productId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MessageResponse> messages;
}

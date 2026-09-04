package ecommerce.modules.message.dto;

import ecommerce.common.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private UUID id;
    private UUID conversationId;
    private UUID senderId;
    private MessageType senderType;
    private String senderName;
    private String content;
    private Boolean isRead;
    private LocalDateTime readAt;
    private Boolean isSystemMessage;
    private String attachmentUrl;
    private LocalDateTime createdAt;
}

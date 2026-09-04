package ecommerce.modules.message.dto;

import ecommerce.common.enums.ConversationCategory;
import ecommerce.common.enums.MessagePriority;
import ecommerce.common.enums.MessageStatus;
import ecommerce.common.enums.MessageType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationRequest {

    @NotBlank(message = "Subject is required")
    private String subject;

    private String initialMessage;

    private ConversationCategory category;

    private MessagePriority priority;

    private MessageType recipientType;

    private UUID orderId;

    private UUID productId;
}

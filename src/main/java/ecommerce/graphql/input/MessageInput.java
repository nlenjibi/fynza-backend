package ecommerce.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MessageInput {
    private java.util.UUID conversationId;
    private java.util.UUID recipientId;
    private String subject;
    private String category;
    private String content;
    private String messageType;
    private List<AttachmentInput> attachments;
    private String priority;
}

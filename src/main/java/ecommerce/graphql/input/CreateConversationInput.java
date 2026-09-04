package ecommerce.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import ecommerce.common.enums.ConversationCategory;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationInput {
    private String subject;
    private ConversationCategory category;
    private String content;
    private java.util.UUID orderId;
    private java.util.UUID productId;
}

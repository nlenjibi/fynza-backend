package ecommerce.modules.message.service;

import ecommerce.common.enums.MessageStatus;
import ecommerce.common.enums.MessageType;
import ecommerce.modules.message.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MessageService {

    ConversationResponse createConversation(UUID userId, MessageType userType, CreateConversationRequest request);

    ConversationResponse getConversation(UUID conversationId, UUID userId);

    Page<ConversationResponse> getUserConversations(UUID userId, MessageStatus status, MessageType type, String search, Pageable pageable);

    ConversationResponse sendMessage(UUID conversationId, UUID senderId, MessageType senderType, String senderName, SendMessageRequest request);

    ConversationResponse replyToConversation(UUID conversationId, UUID senderId, MessageType senderType, String senderName, SendMessageRequest request);

    ConversationResponse updateConversationStatus(UUID conversationId, MessageStatus status);

    ConversationResponse toggleStar(UUID conversationId, UUID userId);

    ConversationResponse togglePin(UUID conversationId, UUID userId);

    void markAsRead(UUID conversationId);

    void deleteConversation(UUID conversationId, UUID userId);

    ConversationStatsResponse getUserStats(UUID userId);

    ConversationStatsResponse getAdminStats();

    Page<ConversationResponse> getAdminConversations(MessageStatus status, String priority, String search, Pageable pageable);
}

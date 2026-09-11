package ecommerce.modules.message.service.impl;

import ecommerce.common.enums.MessageStatus;
import ecommerce.common.enums.MessageType;
import ecommerce.common.exception.BadRequestException;
import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.message.dto.*;
import ecommerce.modules.message.entity.Conversation;
import ecommerce.modules.message.entity.Message;
import ecommerce.modules.message.repository.ConversationRepository;
import ecommerce.modules.message.repository.MessageRepository;
import ecommerce.modules.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageServiceImpl implements MessageService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    @Override
    @Transactional
    public ConversationResponse createConversation(UUID userId, MessageType userType, CreateConversationRequest request) {
        Conversation conversation = Conversation.builder()
                .participantId(userId)
                .participantType(userType)
                .subject(request.getSubject())
                .category(request.getCategory())
                .priority(request.getPriority() != null ? request.getPriority() : ecommerce.common.enums.MessagePriority.MEDIUM)
                .status(MessageStatus.OPEN)
                .type(request.getRecipientType() != null ? request.getRecipientType() : MessageType.SUPPORT)
                .isStarred(false)
                .isPinned(false)
                .unreadCount(0)
                .orderId(request.getOrderId())
                .productId(request.getProductId())
                .build();

        Conversation saved = conversationRepository.save(conversation);

        if (request.getInitialMessage() != null && !request.getInitialMessage().isBlank()) {
            Message message = Message.builder()
                    .conversation(saved)
                    .senderId(userId)
                    .senderType(userType)
                    .senderName("User")
                    .content(request.getInitialMessage())
                    .isRead(true)
                    .readAt(LocalDateTime.now())
                    .isSystemMessage(false)
                    .build();
            messageRepository.save(message);
            saved.setLastMessagePreview(request.getInitialMessage().length() > 100 ? 
                request.getInitialMessage().substring(0, 100) : request.getInitialMessage());
            saved = conversationRepository.save(saved);
        }

        log.info("Created conversation {} for user {}", saved.getId(), userId);
        return toConversationResponse(saved, null);
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponse getConversation(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findByPublicId(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!conversation.getParticipantId().equals(userId)) {
            throw new BadRequestException("You don't have access to this conversation");
        }

        Page<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(
                conversationId, Pageable.unpaged());

        return toConversationResponse(conversation, messages.getContent());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationResponse> getUserConversations(UUID userId, MessageStatus status, MessageType type, String search, Pageable pageable) {
        return conversationRepository.findByParticipantWithFilters(userId, status, type, search, pageable)
                .map(c -> toConversationResponse(c, null));
    }

    @Override
    @Transactional
    public ConversationResponse sendMessage(UUID conversationId, UUID senderId, MessageType senderType, String senderName, SendMessageRequest request) {
        return replyToConversation(conversationId, senderId, senderType, senderName, request);
    }

    @Override
    @Transactional
    public ConversationResponse replyToConversation(UUID conversationId, UUID senderId, MessageType senderType, String senderName, SendMessageRequest request) {
        Conversation conversation = conversationRepository.findByPublicId(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        Message message = Message.builder()
                .conversation(conversation)
                .senderId(senderId)
                .senderType(senderType)
                .senderName(senderName)
                .content(request.getContent())
                .attachmentUrl(request.getAttachmentUrl())
                .isRead(false)
                .isSystemMessage(false)
                .build();

        messageRepository.save(message);

        String preview = request.getContent().length() > 100 ? 
            request.getContent().substring(0, 100) : request.getContent();
        conversation.setLastMessagePreview(preview);
        conversation.setUpdatedAt(java.time.Instant.now());

        if (conversation.getStatus() == MessageStatus.RESOLVED) {
            conversation.setStatus(MessageStatus.OPEN);
        }

        conversationRepository.save(conversation);

        log.info("Added message to conversation {} from {}", conversationId, senderId);
        return toConversationResponse(conversation, null);
    }

    @Override
    @Transactional
    public ConversationResponse updateConversationStatus(UUID conversationId, MessageStatus status) {
        Conversation conversation = conversationRepository.findByPublicId(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        conversation.setStatus(status);
        Conversation saved = conversationRepository.save(conversation);

        log.info("Updated conversation {} status to {}", conversationId, status);
        return toConversationResponse(saved, null);
    }

    @Override
    @Transactional
    public ConversationResponse toggleStar(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findByPublicId(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!conversation.getParticipantId().equals(userId)) {
            throw new BadRequestException("You don't have access to this conversation");
        }

        conversation.setIsStarred(!conversation.getIsStarred());
        Conversation saved = conversationRepository.save(conversation);

        return toConversationResponse(saved, null);
    }

    @Override
    @Transactional
    public ConversationResponse togglePin(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findByPublicId(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!conversation.getParticipantId().equals(userId)) {
            throw new BadRequestException("You don't have access to this conversation");
        }

        conversation.setIsPinned(!conversation.getIsPinned());
        Conversation saved = conversationRepository.save(conversation);

        return toConversationResponse(saved, null);
    }

    @Override
    @Transactional
    public void markAsRead(UUID conversationId) {
        conversationRepository.findByPublicId(conversationId).ifPresent(conversation -> {
            messageRepository.markAllAsRead(conversation.getPublicId());
            conversation.setUnreadCount(0);
            conversationRepository.save(conversation);
            log.info("Marked conversation {} as read", conversationId);
        });
    }

    @Override
    @Transactional
    public void deleteConversation(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findByPublicId(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!conversation.getParticipantId().equals(userId)) {
            throw new BadRequestException("You don't have access to this conversation");
        }

        conversation.setIsActive(false);
        conversationRepository.save(conversation);
        log.info("Soft deleted conversation {}", conversationId);
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationStatsResponse getUserStats(UUID userId) {
        long unread = conversationRepository.countByParticipantIdAndUnreadCountGreaterThan(userId, 0);
        long open = conversationRepository.countByParticipantIdAndStatus(userId, MessageStatus.OPEN);
        long pending = conversationRepository.countByParticipantIdAndStatus(userId, MessageStatus.PENDING);
        long resolved = conversationRepository.countByParticipantIdAndStatus(userId, MessageStatus.RESOLVED);

        return ConversationStatsResponse.builder()
                .unreadCount(unread)
                .openCount(open)
                .pendingCount(pending)
                .resolvedCount(resolved)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationStatsResponse getAdminStats() {
        long unread = conversationRepository.countByStatus(MessageStatus.OPEN);
        long pending = conversationRepository.countByStatus(MessageStatus.PENDING);
        long resolved = conversationRepository.countByStatus(MessageStatus.RESOLVED);

        return ConversationStatsResponse.builder()
                .unreadCount(unread)
                .openCount(unread)
                .pendingCount(pending)
                .resolvedCount(resolved)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationResponse> getAdminConversations(MessageStatus status, String priority, String search, Pageable pageable) {
        return conversationRepository.findAllWithFilters(status, priority, search, pageable)
                .map(c -> toConversationResponse(c, null));
    }

    private ConversationResponse toConversationResponse(Conversation conversation, java.util.List<Message> messages) {
        return ConversationResponse.builder()
                .id(conversation.getPublicId())
                .subject(conversation.getSubject())
                .category(conversation.getCategory())
                .priority(conversation.getPriority())
                .status(conversation.getStatus())
                .type(conversation.getType())
                .participantType(conversation.getParticipantType())
                .isStarred(conversation.getIsStarred())
                .isPinned(conversation.getIsPinned())
                .lastMessagePreview(conversation.getLastMessagePreview())
                .unreadCount(conversation.getUnreadCount())
                .orderId(conversation.getOrderId())
                .productId(conversation.getProductId())
                .createdAt(conversation.getCreatedAt() != null ? conversation.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null)
                .updatedAt(conversation.getUpdatedAt() != null ? conversation.getUpdatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null)
                .messages(messages != null ? messages.stream()
                        .map(this::toMessageResponse)
                        .collect(Collectors.toList()) : null)
                .build();
    }

    private MessageResponse toMessageResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getPublicId())
                .conversationId(message.getConversation().getPublicId())
                .senderId(message.getSenderId())
                .senderType(message.getSenderType())
                .senderName(message.getSenderName())
                .content(message.getContent())
                .isRead(message.getIsRead())
                .readAt(message.getReadAt())
                .isSystemMessage(message.getIsSystemMessage())
                .attachmentUrl(message.getAttachmentUrl())
                .createdAt(message.getCreatedAt() != null ? message.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null)
                .build();
    }
}

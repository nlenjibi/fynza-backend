package ecommerce.graphql.resolver.message;

import ecommerce.common.enums.MessageStatus;
import ecommerce.common.enums.MessageType;
import ecommerce.common.response.PaginatedResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.graphql.dto.ConversationConnection;
import ecommerce.graphql.input.CreateConversationInput;
import ecommerce.graphql.input.PageInput;
import ecommerce.graphql.input.SendMessageInput;
import ecommerce.graphql.input.SortDirection;
import ecommerce.modules.message.dto.ConversationResponse;
import ecommerce.modules.message.dto.ConversationStatsResponse;
import ecommerce.modules.message.dto.CreateConversationRequest;
import ecommerce.modules.message.dto.SendMessageRequest;
import ecommerce.modules.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MessageResolver {

    private final MessageService messageService;

    // =========================================================================
    // AUTHENTICATED QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public ConversationConnection myConversations(@Argument PageInput pagination,
                                                  @Argument String status,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL myConversations(user={})", principal.getId());
        Pageable pageable = toPageable(pagination);
        MessageStatus msgStatus = status != null ? MessageStatus.valueOf(status.toUpperCase()) : null;
        Page<ConversationResponse> page = messageService.getUserConversations(principal.getId(), msgStatus, null, null, pageable);
        return ConversationConnection.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public ConversationResponse conversation(@Argument UUID id,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL conversation(id={}, user={})", id, principal.getId());
        return messageService.getConversation(id, principal.getId());
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public ConversationStatsResponse myMessageStats(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL myMessageStats(user={})", principal.getId());
        return messageService.getUserStats(principal.getId());
    }

    // =========================================================================
    // ADMIN QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ConversationConnection adminConversations(@Argument PageInput pagination,
                                                     @Argument String status,
                                                     @Argument String search) {
        log.info("GQL adminConversations");
        Pageable pageable = toPageable(pagination);
        MessageStatus msgStatus = status != null ? MessageStatus.valueOf(status.toUpperCase()) : null;
        Page<ConversationResponse> page = messageService.getAdminConversations(msgStatus, null, search, pageable);
        return ConversationConnection.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ConversationStatsResponse adminMessageStats() {
        log.info("GQL adminMessageStats");
        return messageService.getAdminStats();
    }

    // =========================================================================
    // AUTHENTICATED MUTATIONS
    // =========================================================================

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public ConversationResponse createConversation(@Argument CreateConversationInput input,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL createConversation(user={})", principal.getId());
        CreateConversationRequest request = CreateConversationRequest.builder()
                .subject(input.getSubject())
                .category(input.getCategory())
                .initialMessage(input.getContent())
                .orderId(input.getOrderId())
                .productId(input.getProductId())
                .build();
        return messageService.createConversation(principal.getId(), MessageType.CUSTOMER, request);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public ConversationResponse replyToConversation(@Argument UUID id,
                                                    @Argument SendMessageInput input,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL replyToConversation(id={}, user={})", id, principal.getId());
        SendMessageRequest request = SendMessageRequest.builder()
                .content(input.getContent())
                .build();
        return messageService.replyToConversation(id, principal.getId(), MessageType.CUSTOMER, principal.getId().toString(), request);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean markConversationAsRead(@Argument UUID id) {
        log.info("GQL markConversationAsRead(id={})", id);
        messageService.markAsRead(id);
        return true;
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public ConversationResponse toggleConversationStar(@Argument UUID id,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL toggleConversationStar(id={}, user={})", id, principal.getId());
        return messageService.toggleStar(id, principal.getId());
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public boolean deleteConversation(@Argument UUID id,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL deleteConversation(id={}, user={})", id, principal.getId());
        messageService.deleteConversation(id, principal.getId());
        return true;
    }

    // =========================================================================
    // ADMIN MUTATIONS
    // =========================================================================

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ConversationResponse adminReplyToConversation(@Argument UUID id,
                                                         @Argument SendMessageInput input) {
        log.info("GQL adminReplyToConversation(id={})", id);
        SendMessageRequest request = SendMessageRequest.builder()
                .content(input.getContent())
                .build();
        UUID adminId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        return messageService.replyToConversation(id, adminId, MessageType.SUPPORT, "Fynza Admin", request);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ConversationResponse updateConversationStatus(@Argument UUID id, @Argument String status) {
        log.info("GQL updateConversationStatus(id={}, status={})", id, status);
        return messageService.updateConversationStatus(id, MessageStatus.valueOf(status.toUpperCase()));
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public boolean adminDeleteConversation(@Argument UUID id) {
        log.info("GQL adminDeleteConversation(id={})", id);
        UUID adminId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        messageService.deleteConversation(id, adminId);
        return true;
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private Pageable toPageable(PageInput input) {
        if (input == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        Sort sort = input.getDirection() == SortDirection.DESC
                ? Sort.by(input.getSortBy()).descending()
                : Sort.by(input.getSortBy()).ascending();
        return PageRequest.of(input.getPage(), input.getSize(), sort);
    }
}

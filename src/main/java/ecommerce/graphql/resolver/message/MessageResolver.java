package ecommerce.graphql.resolver.message;

import ecommerce.common.enums.MessageStatus;
import ecommerce.common.response.PaginatedResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.graphql.dto.ConversationConnection;
import ecommerce.graphql.input.PageInput;
import ecommerce.graphql.input.SortDirection;
import ecommerce.modules.message.dto.ConversationResponse;
import ecommerce.modules.message.dto.ConversationStatsResponse;
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
    // UX STATE MUTATIONS
    // =========================================================================

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

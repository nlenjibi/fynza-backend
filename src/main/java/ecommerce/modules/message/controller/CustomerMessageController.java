package ecommerce.modules.message.controller;

import ecommerce.common.enums.MessageStatus;
import ecommerce.common.enums.MessageType;
import ecommerce.common.response.ApiResponse;
import ecommerce.modules.message.dto.*;
import ecommerce.modules.message.service.MessageService;
import ecommerce.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/customer/messages")
@RequiredArgsConstructor
@Tag(name = "Customer Messages", description = "Customer message management endpoints")
public class CustomerMessageController {

    private final MessageService messageService;

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get customer conversations", description = "Get all conversations for the customer")
    public ResponseEntity<ApiResponse<Page<ConversationResponse>>> getConversations(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @RequestParam(required = false) MessageStatus status,
            @RequestParam(required = false) MessageType type,
            @RequestParam(required = false) String search) {
        UUID customerId = UUID.fromString(principal.getId().toString());
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<ConversationResponse> conversations = messageService.getUserConversations(customerId, status, type, search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Conversations retrieved successfully", conversations));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get message stats", description = "Get message statistics for customer")
    public ResponseEntity<ApiResponse<ConversationStatsResponse>> getStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID customerId = UUID.fromString(principal.getId().toString());
        return ResponseEntity.ok(ApiResponse.success("Stats retrieved", messageService.getUserStats(customerId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get conversation", description = "Get conversation details")
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversation(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID customerId = UUID.fromString(principal.getId().toString());
        ConversationResponse conversation = messageService.getConversation(id, customerId);
        messageService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Conversation retrieved", conversation));
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Create conversation", description = "Start a new conversation")
    public ResponseEntity<ApiResponse<ConversationResponse>> createConversation(
            @Valid @RequestBody CreateConversationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID customerId = UUID.fromString(principal.getId().toString());
        ConversationResponse conversation = messageService.createConversation(customerId, MessageType.CUSTOMER, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Conversation created successfully", conversation));
    }

    @PostMapping("/{id}/reply")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Reply to conversation", description = "Send a reply to a conversation")
    public ResponseEntity<ApiResponse<ConversationResponse>> reply(
            @PathVariable UUID id,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID customerId = UUID.fromString(principal.getId().toString());
        ConversationResponse conversation = messageService.replyToConversation(
                id, customerId, MessageType.CUSTOMER, principal.getEmail(), request);
        return ResponseEntity.ok(ApiResponse.success("Reply sent successfully", conversation));
    }

    @PatchMapping("/{id}/star")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Toggle star", description = "Toggle star on conversation")
    public ResponseEntity<ApiResponse<ConversationResponse>> toggleStar(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID customerId = UUID.fromString(principal.getId().toString());
        return ResponseEntity.ok(ApiResponse.success("Star toggled", messageService.toggleStar(id, customerId)));
    }

    @PatchMapping("/{id}/pin")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Toggle pin", description = "Toggle pin on conversation")
    public ResponseEntity<ApiResponse<ConversationResponse>> togglePin(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID customerId = UUID.fromString(principal.getId().toString());
        return ResponseEntity.ok(ApiResponse.success("Pin toggled", messageService.togglePin(id, customerId)));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Mark as read", description = "Mark conversation as read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable UUID id) {
        messageService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Marked as read", null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Delete conversation", description = "Delete a conversation")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID customerId = UUID.fromString(principal.getId().toString());
        messageService.deleteConversation(id, customerId);
        return ResponseEntity.ok(ApiResponse.success("Conversation deleted successfully", null));
    }
}

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
@RequestMapping("/api/v1/sellers/messages")
@RequiredArgsConstructor
@Tag(name = "Seller Messages", description = "Seller message management endpoints")
public class SellerMessageController {

    private final MessageService messageService;

    @GetMapping
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get seller conversations", description = "Get all conversations for the seller")
    public ResponseEntity<ApiResponse<Page<ConversationResponse>>> getConversations(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @RequestParam(required = false) MessageStatus status,
            @RequestParam(required = false) String search) {
        UUID sellerId = UUID.fromString(principal.getId().toString());
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<ConversationResponse> conversations = messageService.getUserConversations(sellerId, status, null, search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Conversations retrieved successfully", conversations));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get message stats", description = "Get message statistics for seller")
    public ResponseEntity<ApiResponse<ConversationStatsResponse>> getStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID sellerId = UUID.fromString(principal.getId().toString());
        return ResponseEntity.ok(ApiResponse.success("Stats retrieved", messageService.getUserStats(sellerId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Get conversation", description = "Get conversation details")
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversation(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID sellerId = UUID.fromString(principal.getId().toString());
        ConversationResponse conversation = messageService.getConversation(id, sellerId);
        messageService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Conversation retrieved", conversation));
    }

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Create conversation", description = "Start a new conversation")
    public ResponseEntity<ApiResponse<ConversationResponse>> createConversation(
            @Valid @RequestBody CreateConversationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID sellerId = UUID.fromString(principal.getId().toString());
        ConversationResponse conversation = messageService.createConversation(sellerId, MessageType.SELLER, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Conversation created successfully", conversation));
    }

    @PostMapping("/{id}/reply")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Reply to conversation", description = "Send a reply to a conversation")
    public ResponseEntity<ApiResponse<ConversationResponse>> reply(
            @PathVariable UUID id,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID sellerId = UUID.fromString(principal.getId().toString());
        ConversationResponse conversation = messageService.replyToConversation(
                id, sellerId, MessageType.SELLER, principal.getEmail(), request);
        return ResponseEntity.ok(ApiResponse.success("Reply sent successfully", conversation));
    }

    @PatchMapping("/{id}/star")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Toggle star", description = "Toggle star on conversation")
    public ResponseEntity<ApiResponse<ConversationResponse>> toggleStar(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID sellerId = UUID.fromString(principal.getId().toString());
        return ResponseEntity.ok(ApiResponse.success("Star toggled", messageService.toggleStar(id, sellerId)));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Mark as read", description = "Mark conversation as read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable UUID id) {
        messageService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Marked as read", null));
    }
}

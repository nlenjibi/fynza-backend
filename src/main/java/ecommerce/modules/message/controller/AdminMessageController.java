package ecommerce.modules.message.controller;

import ecommerce.common.enums.MessageStatus;
import ecommerce.common.enums.MessageType;
import ecommerce.common.response.ApiResponse;
import ecommerce.modules.message.dto.*;
import ecommerce.modules.message.service.MessageService;
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
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/messages")
@RequiredArgsConstructor
@Tag(name = "Admin Messages", description = "Admin message management endpoints")
public class AdminMessageController {

    private final MessageService messageService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all conversations", description = "Get all conversations with filters")
    public ResponseEntity<ApiResponse<Page<ConversationResponse>>> getAllConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @RequestParam(required = false) MessageStatus status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<ConversationResponse> conversations = messageService.getAdminConversations(status, priority, search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Conversations retrieved successfully", conversations));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get message stats", description = "Get message statistics")
    public ResponseEntity<ApiResponse<ConversationStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success("Stats retrieved", messageService.getAdminStats()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get conversation", description = "Get conversation details")
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversation(@PathVariable UUID id) {
        ConversationResponse conversation = messageService.getConversation(id, UUID.fromString("00000000-0000-0000-0000-000000000000"));
        return ResponseEntity.ok(ApiResponse.success("Conversation retrieved", conversation));
    }

    @PostMapping("/{id}/reply")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reply to conversation", description = "Send a reply to a conversation")
    public ResponseEntity<ApiResponse<ConversationResponse>> reply(
            @PathVariable UUID id,
            @Valid @RequestBody SendMessageRequest request) {
        ConversationResponse conversation = messageService.replyToConversation(
                id, UUID.fromString("00000000-0000-0000-0000-000000000000"),
                MessageType.SUPPORT, "Fynza Admin", request);
        return ResponseEntity.ok(ApiResponse.success("Reply sent successfully", conversation));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update conversation status", description = "Update conversation status")
    public ResponseEntity<ApiResponse<ConversationResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateConversationStatusRequest request) {
        ConversationResponse conversation = messageService.updateConversationStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Status updated successfully", conversation));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete conversation", description = "Delete a conversation")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @PathVariable UUID id,
            @RequestParam UUID adminId) {
        messageService.deleteConversation(id, adminId);
        return ResponseEntity.ok(ApiResponse.success("Conversation deleted successfully", null));
    }
}

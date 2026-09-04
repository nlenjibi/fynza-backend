package ecommerce.modules.contact.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.response.PaginatedResponse;
import ecommerce.modules.contact.dto.ContactMessageRequest;
import ecommerce.modules.contact.dto.ContactMessageResponse;
import ecommerce.modules.contact.dto.ContactResponseRequest;
import ecommerce.modules.contact.dto.ContactStats;
import ecommerce.modules.contact.service.ContactService;
import ecommerce.common.enums.ContactCategory;
import ecommerce.common.enums.ContactPriority;
import ecommerce.common.enums.ContactStatus;
import ecommerce.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/v1/contact")
@RequiredArgsConstructor
@Tag(name = "Contact Management", description = "APIs for managing contact messages")
public class ContactController {

    private final ContactService contactService;

    @PostMapping
    @Operation(summary = "Submit contact message", description = "Submit a new contact message - public endpoint")
    public ResponseEntity<ApiResponse<ContactMessageResponse>> submitMessage(
            @Valid @RequestBody ContactMessageRequest request) {
        ContactMessageResponse response = contactService.createMessage(request);
        return ResponseEntity.ok(ApiResponse.success("Message submitted successfully. We'll get back to you soon!", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all messages", description = "Retrieve all contact messages with pagination - ADMIN only")
    public ResponseEntity<ApiResponse<PaginatedResponse<ContactMessageResponse>>> getAllMessages(
            @RequestParam(required = false) ContactStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ContactMessageResponse> messages = contactService.getAllMessages(status, pageable);

        return ResponseEntity.ok(ApiResponse.success("Messages retrieved successfully", PaginatedResponse.from(messages)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get message by ID", description = "Retrieve contact message details - ADMIN only")
    public ResponseEntity<ApiResponse<ContactMessageResponse>> getMessageById(@PathVariable UUID id) {
        ContactMessageResponse message = contactService.getMessageById(id);
        return ResponseEntity.ok(ApiResponse.success("Message retrieved successfully", message));
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Get message status", description = "Check contact message status - public endpoint")
    public ResponseEntity<ApiResponse<ContactStatus>> getMessageStatus(@PathVariable UUID id) {
        ContactMessageResponse message = contactService.getMessageById(id);
        return ResponseEntity.ok(ApiResponse.success("Status retrieved successfully", message.getStatus()));
    }

    @PutMapping("/{id}/respond")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Respond to message", description = "Admin responds to contact message - ADMIN only")
    public ResponseEntity<ApiResponse<ContactMessageResponse>> respondToMessage(
            @PathVariable UUID id,
            @Valid @RequestBody ContactResponseRequest request) {
        ContactMessageResponse response = contactService.respondToMessage(id, request);
        return ResponseEntity.ok(ApiResponse.success("Response sent successfully", response));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update message status", description = "Update contact message status - ADMIN only")
    public ResponseEntity<ApiResponse<ContactMessageResponse>> updateMessageStatus(
            @PathVariable UUID id,
            @RequestParam ContactStatus status) {
        ContactMessageResponse response = contactService.updateMessageStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Status updated successfully", response));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign message", description = "Assign contact message to admin - ADMIN only")
    public ResponseEntity<ApiResponse<ContactMessageResponse>> assignMessage(
            @PathVariable UUID id,
            @RequestParam UUID assignedToId) {
        ContactMessageResponse response = contactService.assignMessage(id, assignedToId);
        return ResponseEntity.ok(ApiResponse.success("Message assigned successfully", response));
    }

    @PutMapping("/{id}/categorize")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Categorize message", description = "Set category and priority for contact message - ADMIN only")
    public ResponseEntity<ApiResponse<ContactMessageResponse>> categorizeMessage(
            @PathVariable UUID id,
            @RequestParam(required = false) ContactCategory category,
            @RequestParam(required = false) ContactPriority priority) {
        ContactMessageResponse response = contactService.categorizeMessage(id, category, priority);
        return ResponseEntity.ok(ApiResponse.success("Message categorized successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete message", description = "Soft delete contact message - ADMIN only")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(@PathVariable UUID id) {
        contactService.deleteMessage(id);
        return ResponseEntity.ok(ApiResponse.success("Message deleted successfully", null));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search messages", description = "Search contact messages by email, name, or subject - ADMIN only")
    public ResponseEntity<ApiResponse<PaginatedResponse<ContactMessageResponse>>> searchMessages(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ContactMessageResponse> messages = contactService.searchMessages(query, pageable);

        return ResponseEntity.ok(ApiResponse.success("Search results retrieved successfully", PaginatedResponse.from(messages)));
    }

    @GetMapping("/my-assigned")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get my assigned messages", description = "Get messages assigned to current admin - ADMIN only")
    public ResponseEntity<ApiResponse<PaginatedResponse<ContactMessageResponse>>> getMyAssignedMessages(
            @RequestParam(required = false) ContactStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID currentAdminId = principal.getId();

        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ContactMessageResponse> messages;
        if (status != null) {
            messages = contactService.getMessagesByAssignedToAndStatus(currentAdminId, status, pageable);
        } else {
            messages = contactService.getMessagesByAssignedTo(currentAdminId, pageable);
        }
        
        return ResponseEntity.ok(ApiResponse.success("My assigned messages retrieved successfully", PaginatedResponse.from(messages)));
    }

    @GetMapping("/unassigned")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get unassigned messages", description = "Get unassigned contact messages - ADMIN only")
    public ResponseEntity<ApiResponse<PaginatedResponse<ContactMessageResponse>>> getUnassignedMessages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ContactMessageResponse> messages = contactService.getUnassignedMessages(pageable);
        
        return ResponseEntity.ok(ApiResponse.success("Unassigned messages retrieved successfully", PaginatedResponse.from(messages)));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get message statistics", description = "Get count of messages by status - ADMIN only")
    public ResponseEntity<ApiResponse<ContactStats>> getMessageStats() {
        ContactStats stats = contactService.getMessageStats();
        return ResponseEntity.ok(ApiResponse.success("Statistics retrieved successfully", stats));
    }
}

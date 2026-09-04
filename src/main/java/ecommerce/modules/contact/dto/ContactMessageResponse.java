package ecommerce.modules.contact.dto;

import ecommerce.common.enums.ContactCategory;
import ecommerce.common.enums.ContactPriority;
import ecommerce.common.enums.ContactStatus;
import ecommerce.modules.contact.entity.ContactMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactMessageResponse {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String subject;
    private String message;
    private ContactStatus status;
    private ContactPriority priority;
    private ContactCategory category;
    private UUID assignedTo;
    private String adminResponse;
    private LocalDateTime respondedAt;
    private String respondedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ContactMessageResponse from(ContactMessage message) {
        return ContactMessageResponse.builder()
                .id(message.getId())
                .name(message.getName())
                .email(message.getEmail())
                .phone(message.getPhone())
                .subject(message.getSubject())
                .message(message.getMessage())
                .status(message.getStatus())
                .priority(message.getPriority())
                .category(message.getCategory())
                .assignedTo(message.getAssignedTo())
                .adminResponse(message.getAdminResponse())
                .respondedAt(message.getRespondedAt())
                .respondedBy(message.getRespondedBy())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }
}

package ecommerce.modules.contact.entity;

import ecommerce.common.base.BaseEntity;
import ecommerce.common.enums.ContactCategory;
import ecommerce.common.enums.ContactPriority;
import ecommerce.common.enums.ContactStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contact_messages", indexes = {
    @Index(name = "idx_contact_status", columnList = "status"),
    @Index(name = "idx_contact_priority", columnList = "priority"),
    @Index(name = "idx_contact_category", columnList = "category"),
    @Index(name = "idx_contact_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ContactMessage extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ContactStatus status = ContactStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private ContactPriority priority = ContactPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    @Builder.Default
    private ContactCategory category = ContactCategory.GENERAL_INQUIRY;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "admin_response", columnDefinition = "TEXT")
    private String adminResponse;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "responded_by")
    private String respondedBy;

}

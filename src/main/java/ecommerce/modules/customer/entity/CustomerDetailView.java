package ecommerce.modules.customer.entity;

import ecommerce.modules.customer.enums.CustomerStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "v_customer_detail")
@Getter
@NoArgsConstructor
public class CustomerDetailView {

    @Id
    private Long id;

    @Column(name = "public_id")
    private UUID publicId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "customer_number")
    private String customerNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CustomerStatus status;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // From users
    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    // From customer_preferences (nullable — LEFT JOIN)
    @Column(name = "pref_public_id")
    private UUID prefPublicId;

    @Column(name = "language")
    private String language;

    @Column(name = "currency")
    private String currency;

    @Column(name = "marketing_opt_in")
    private Boolean marketingOptIn;

    @Column(name = "email_notifications")
    private Boolean emailNotifications;

    @Column(name = "sms_notifications")
    private Boolean smsNotifications;

    @Column(name = "push_notifications")
    private Boolean pushNotifications;

    @Column(name = "pref_updated_at")
    private Instant prefUpdatedAt;
}

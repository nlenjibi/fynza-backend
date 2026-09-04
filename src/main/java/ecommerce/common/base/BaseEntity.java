package ecommerce.common.base;

import ecommerce.common.util.UuidV7Generator;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * BIGINT id is the internal database primary key — used for all FK relationships and joins.
 * publicId (UUIDv7) is the only identifier exposed via the API — prevents enumeration attacks
 * and decouples API identity from database identity.
 */
@Data
@NoArgsConstructor
@SuperBuilder
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Column(name = "public_id", unique = true, nullable = false, updatable = false, columnDefinition = "UUID")
    protected UUID publicId;

    @Column(nullable = false)
    protected Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    protected LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    protected LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (publicId == null) {
            publicId = UuidV7Generator.generate();
        }
        if (isActive == null) {
            isActive = true;
        }
    }
}

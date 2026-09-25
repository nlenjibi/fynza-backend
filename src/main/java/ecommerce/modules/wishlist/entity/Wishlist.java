package ecommerce.modules.wishlist.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "wishlists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "items")
public class Wishlist {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "id", insertable = false, updatable = false)
    private UUID publicId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "guest_token_hash", length = 64)
    private String guestTokenHash;

    @Column(name = "name", nullable = false, length = 100)
    @Builder.Default
    private String name = "My Wishlist";

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private WishlistStatus status = WishlistStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    @Builder.Default
    private WishlistVisibility visibility = WishlistVisibility.PRIVATE;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "share_token_hash", length = 64, unique = true)
    private String shareTokenHash;

    @OneToMany(mappedBy = "wishlist", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @Getter(lombok.AccessLevel.NONE)
    private List<WishlistItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public List<WishlistItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isGuest() {
        return customerId == null && guestTokenHash != null;
    }
}

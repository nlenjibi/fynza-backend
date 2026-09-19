package ecommerce.modules.wishlist.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "v_wishlist_summary")
@Getter
@NoArgsConstructor
public class WishlistSummaryView {

    @Id
    private Long id;

    @Column(name = "public_id")
    private UUID publicId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "status")
    private String status;

    @Column(name = "visibility")
    private String visibility;

    @Column(name = "is_default")
    private Boolean isDefault;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "item_count")
    private Integer itemCount;
}

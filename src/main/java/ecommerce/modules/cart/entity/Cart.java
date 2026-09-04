package ecommerce.modules.cart.entity;

import ecommerce.common.base.BaseEntity;
import ecommerce.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "carts", indexes = {
        @Index(name = "idx_cart_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Cart extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true, insertable = false, updatable = false)
    private User user;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    @Column(name = "is_checked_out")
    @Builder.Default
    private Boolean isCheckedOut = false;

    @Column(name = "checked_out_at")
    private LocalDateTime checkedOutAt;

    @Column(name = "is_abandoned")
    @Builder.Default
    private Boolean isAbandoned = false;

    @Column(name = "abandoned_at")
    private LocalDateTime abandonedAt;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    public List<CartItem> getItems() {
        return items;
    }


}

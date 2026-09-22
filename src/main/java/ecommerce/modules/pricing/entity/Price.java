package ecommerce.modules.pricing.entity;

import ecommerce.modules.pricing.enums.PriceStatus;
import ecommerce.modules.pricing.enums.SupportedCurrency;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "prices", indexes = {
        @Index(name = "idx_prices_product_id",    columnList = "product_id"),
        @Index(name = "idx_prices_variant_id",    columnList = "variant_id"),
        @Index(name = "idx_prices_price_list_id", columnList = "price_list_id"),
        @Index(name = "idx_prices_status",        columnList = "status"),
        @Index(name = "idx_prices_currency",      columnList = "currency"),
        @Index(name = "idx_prices_valid_from",    columnList = "valid_from"),
        @Index(name = "idx_prices_valid_until",   columnList = "valid_until"),
        @Index(name = "idx_prices_is_active",     columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"tiers", "overrides", "history", "priceList"})
public class Price {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_list_id", nullable = false)
    private PriceList priceList;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "variant_id", updatable = false)
    private UUID variantId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "sale_amount", precision = 19, scale = 4)
    private BigDecimal saleAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private SupportedCurrency currency = SupportedCurrency.GHS;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PriceStatus status = PriceStatus.DRAFT;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_until")
    private Instant validUntil;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "price", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PriceTier> tiers = new ArrayList<>();

    @OneToMany(mappedBy = "price", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PriceHistory> history = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        publicId  = UUID.randomUUID();
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status   == null) status   = PriceStatus.DRAFT;
        if (currency == null) currency = SupportedCurrency.GHS;
        if (isActive == null) isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

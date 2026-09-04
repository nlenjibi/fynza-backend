package ecommerce.modules.admin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entity for tracking search analytics data.
 * Records search queries, result counts, and user interactions.
 */
@Entity
@Table(name = "search_analytics", indexes = {
    @Index(name = "idx_search_query", columnList = "search_query"),
    @Index(name = "idx_search_date", columnList = "search_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "search_query", nullable = false, length = 500)
    private String searchQuery;

    @Column(name = "search_date", nullable = false)
    private LocalDate searchDate;

    @Column(name = "search_count", nullable = false)
    @Builder.Default
    private Integer searchCount = 0;

    @Column(name = "result_count", nullable = false)
    @Builder.Default
    private Integer resultCount = 0;

    @Column(name = "click_count", nullable = false)
    @Builder.Default
    private Integer clickCount = 0;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "search_type", length = 50)
    private String searchType;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "avg_response_time_ms")
    private Long avgResponseTimeMs;

    @Column(name = "is_zero_results", nullable = false)
    @Builder.Default
    private Boolean isZeroResults = false;

    @PrePersist
    protected void onCreate() {
        publicId = UUID.randomUUID();
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (isActive == null) isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

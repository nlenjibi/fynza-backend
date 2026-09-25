package ecommerce.modules.search.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "search_event", indexes = {
        @Index(name = "idx_search_event_user_id",    columnList = "user_id"),
        @Index(name = "idx_search_event_created_at", columnList = "created_at"),
        @Index(name = "idx_search_event_type",       columnList = "event_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchEvent {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "query", nullable = false, length = 500)
    private String query;

    @Column(name = "normalized_query", length = 500)
    private String normalizedQuery;

    @Column(name = "result_count", nullable = false)
    @Builder.Default
    private Integer resultCount = 0;

    @Column(name = "sort_by", length = 50)
    private String sortBy;

    @Column(name = "selected_product_id")
    private UUID selectedProductId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        createdAt = Instant.now();
    }
}

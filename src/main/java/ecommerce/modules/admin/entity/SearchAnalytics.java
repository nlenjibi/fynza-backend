package ecommerce.modules.admin.entity;

import ecommerce.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

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
@SuperBuilder
public class SearchAnalytics extends BaseEntity {

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
}

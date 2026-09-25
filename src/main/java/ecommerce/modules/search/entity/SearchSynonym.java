package ecommerce.modules.search.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "search_synonym", indexes = {
        @Index(name = "idx_synonym_term",   columnList = "term"),
        @Index(name = "idx_synonym_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchSynonym {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "term", nullable = false, unique = true, length = 200)
    private String term;

    @Column(name = "synonyms", length = 1000)
    private String synonyms;

    @Column(name = "locale", nullable = false, length = 10)
    @Builder.Default
    private String locale = "en";

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

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

    @Transient
    public List<String> getSynonymList() {
        if (synonyms == null || synonyms.isBlank()) return List.of();
        return Arrays.stream(synonyms.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    public void setSynonymList(List<String> list) {
        this.synonyms = list == null ? "" : String.join(",", list);
    }
}

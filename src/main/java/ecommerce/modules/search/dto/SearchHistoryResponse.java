package ecommerce.modules.search.dto;

import java.time.Instant;
import java.util.UUID;

public record SearchHistoryResponse(UUID id, String query, Instant createdAt) {}

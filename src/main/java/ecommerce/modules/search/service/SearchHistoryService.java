package ecommerce.modules.search.service;

import ecommerce.modules.search.dto.SearchHistoryResponse;
import ecommerce.modules.search.entity.SearchHistory;
import ecommerce.modules.search.repository.SearchHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchHistoryService {

    private final SearchHistoryRepository historyRepository;

    @Async
    @Transactional
    public void save(UUID userId, String query) {
        if (userId == null || query == null || query.isBlank()) return;
        historyRepository.save(SearchHistory.builder()
                .userId(userId)
                .query(query.trim())
                .build());
    }

    @Transactional(readOnly = true)
    public List<SearchHistoryResponse> getHistory(UUID userId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return historyRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, safeLimit))
                .stream()
                .map(h -> new SearchHistoryResponse(h.getId(), h.getQuery(), h.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void clearHistory(UUID userId) {
        historyRepository.deleteByUserId(userId);
        log.info("Search history cleared for user {}", userId);
    }
}

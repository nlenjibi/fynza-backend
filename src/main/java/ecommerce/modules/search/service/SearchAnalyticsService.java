package ecommerce.modules.search.service;

import ecommerce.modules.search.entity.SearchAnalytics;
import ecommerce.modules.search.repository.SearchAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchAnalyticsService {

    private final SearchAnalyticsRepository searchAnalyticsRepository;

    @Async
    @Transactional
    public void trackSearch(String query, int resultCount, String searchType,
                            UUID userId, String sessionId, String ipAddress,
                            Long responseTimeMs) {
        LocalDate today = LocalDate.now();
        searchAnalyticsRepository.findBySearchQueryAndSearchDate(query.toLowerCase(), today)
                .ifPresentOrElse(
                        existing -> {
                            existing.setSearchCount(existing.getSearchCount() + 1);
                            existing.setResultCount(resultCount);
                            searchAnalyticsRepository.save(existing);
                        },
                        () -> searchAnalyticsRepository.save(SearchAnalytics.builder()
                                .searchQuery(query.toLowerCase())
                                .searchDate(today)
                                .searchCount(1)
                                .resultCount(resultCount)
                                .searchType(searchType)
                                .userId(userId)
                                .sessionId(sessionId)
                                .ipAddress(ipAddress)
                                .avgResponseTimeMs(responseTimeMs)
                                .isZeroResults(resultCount == 0)
                                .build())
                );
    }

    @Transactional
    public void trackClick(String query) {
        LocalDate today = LocalDate.now();
        searchAnalyticsRepository.findBySearchQueryAndSearchDate(query.toLowerCase(), today)
                .ifPresent(a -> {
                    a.setClickCount(a.getClickCount() + 1);
                    searchAnalyticsRepository.save(a);
                });
    }

    @Transactional(readOnly = true)
    public Page<SearchAnalytics> getTopSearches(int days, Pageable pageable) {
        LocalDate start = LocalDate.now().minusDays(days);
        return searchAnalyticsRepository.findTopSearches(start, LocalDate.now(), pageable);
    }

    @Transactional(readOnly = true)
    public List<Object[]> getMostPopularSearches(int limit) {
        return searchAnalyticsRepository.findMostPopularSearches(Pageable.ofSize(limit));
    }

    @Transactional(readOnly = true)
    public List<Object[]> getMostClickedSearches(int limit) {
        return searchAnalyticsRepository.findMostClickedSearches(Pageable.ofSize(limit));
    }

    @Transactional(readOnly = true)
    public List<Object[]> getSearchTrends(int days) {
        return searchAnalyticsRepository.getSearchTrends(LocalDate.now().minusDays(days));
    }

    @Transactional(readOnly = true)
    public List<Object[]> getSearchTypeDistribution(int days) {
        return searchAnalyticsRepository.getSearchTypeDistribution(LocalDate.now().minusDays(days));
    }

    @Transactional(readOnly = true)
    public double getZeroResultRate(int days) {
        long zeroResults = searchAnalyticsRepository.countZeroResultSearches(LocalDate.now().minusDays(days));
        long total = searchAnalyticsRepository.count();
        return total > 0 ? (double) zeroResults / total * 100 : 0;
    }
}

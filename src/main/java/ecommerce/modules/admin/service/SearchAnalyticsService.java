package ecommerce.modules.admin.service;

import ecommerce.modules.admin.entity.SearchAnalytics;
import ecommerce.modules.admin.repository.SearchAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Service for managing search analytics.
 * Tracks search queries, clicks, and provides analytics data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchAnalyticsService {

    private final SearchAnalyticsRepository searchAnalyticsRepository;

    /**
     * Tracks a search query asynchronously.
     */
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
                            existing.setClickCount(existing.getClickCount());
                            searchAnalyticsRepository.save(existing);
                        },
                        () -> {
                            SearchAnalytics analytics = SearchAnalytics.builder()
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
                                    .build();
                            searchAnalyticsRepository.save(analytics);
                        }
                );
    }

    /**
     * Tracks a search click.
     */
    @Transactional
    public void trackClick(String query) {
        LocalDate today = LocalDate.now();
        searchAnalyticsRepository.findBySearchQueryAndSearchDate(query.toLowerCase(), today)
                .ifPresent(analytics -> {
                    analytics.setClickCount(analytics.getClickCount() + 1);
                    searchAnalyticsRepository.save(analytics);
                });
    }

    /**
     * Gets top searches within a time period.
     */
    @Transactional(readOnly = true)
    public Page<SearchAnalytics> getTopSearches(int days, Pageable pageable) {
        LocalDate start = LocalDate.now().minusDays(days);
        return searchAnalyticsRepository.findTopSearches(start, LocalDate.now(), pageable);
    }

    /**
     * Gets most popular searches.
     */
    @Transactional(readOnly = true)
    public java.util.List<Object[]> getMostPopularSearches(int limit) {
        return searchAnalyticsRepository.findMostPopularSearches(Pageable.ofSize(limit));
    }

    /**
     * Gets most clicked searches.
     */
    @Transactional(readOnly = true)
    public java.util.List<Object[]> getMostClickedSearches(int limit) {
        return searchAnalyticsRepository.findMostClickedSearches(Pageable.ofSize(limit));
    }

    /**
     * Gets search trends over time.
     */
    @Transactional(readOnly = true)
    public java.util.List<Object[]> getSearchTrends(int days) {
        LocalDate since = LocalDate.now().minusDays(days);
        return searchAnalyticsRepository.getSearchTrends(since);
    }

    /**
     * Gets search type distribution.
     */
    @Transactional(readOnly = true)
    public java.util.List<Object[]> getSearchTypeDistribution(int days) {
        LocalDate since = LocalDate.now().minusDays(days);
        return searchAnalyticsRepository.getSearchTypeDistribution(since);
    }

    /**
     * Gets zero result search rate.
     */
    @Transactional(readOnly = true)
    public double getZeroResultRate(int days) {
        LocalDate since = LocalDate.now().minusDays(days);
        long zeroResults = searchAnalyticsRepository.countZeroResultSearches(since);
        long total = searchAnalyticsRepository.count();
        return total > 0 ? (double) zeroResults / total * 100 : 0;
    }
}

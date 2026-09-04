package ecommerce.modules.admin.repository;

import ecommerce.modules.admin.entity.SearchAnalytics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Repository for SearchAnalytics entity.
 */
@Repository
public interface SearchAnalyticsRepository extends JpaRepository<SearchAnalytics, Long> {

    Optional<SearchAnalytics> findBySearchQueryAndSearchDate(String query, LocalDate date);

    @Query("SELECT s FROM SearchAnalytics s WHERE s.searchDate BETWEEN :start AND :end ORDER BY s.searchCount DESC")
    Page<SearchAnalytics> findTopSearches(LocalDate start, LocalDate end, Pageable pageable);

    @Query("SELECT s.searchQuery, SUM(s.searchCount) as total FROM SearchAnalytics s GROUP BY s.searchQuery ORDER BY total DESC")
    java.util.List<Object[]> findMostPopularSearches(Pageable pageable);

    @Query("SELECT s.searchQuery, AVG(s.avgResponseTimeMs) as avgTime FROM SearchAnalytics s WHERE s.avgResponseTimeMs IS NOT NULL GROUP BY s.searchQuery ORDER BY avgTime ASC")
    java.util.List<Object[]> findFastestSearches(Pageable pageable);

    @Query("SELECT s.searchQuery, SUM(s.clickCount) as clicks FROM SearchAnalytics s GROUP BY s.searchQuery ORDER BY clicks DESC")
    java.util.List<Object[]> findMostClickedSearches(Pageable pageable);

    @Query("SELECT s.searchType, SUM(s.searchCount) FROM SearchAnalytics s WHERE s.searchDate >= :since GROUP BY s.searchType")
    java.util.List<Object[]> getSearchTypeDistribution(LocalDate since);

    @Query("SELECT s.searchDate, SUM(s.searchCount) FROM SearchAnalytics s WHERE s.searchDate >= :since GROUP BY s.searchDate ORDER BY s.searchDate")
    java.util.List<Object[]> getSearchTrends(LocalDate since);

    @Query("SELECT COUNT(s) FROM SearchAnalytics s WHERE s.isZeroResults = true AND s.searchDate >= :since")
    long countZeroResultSearches(LocalDate since);
}

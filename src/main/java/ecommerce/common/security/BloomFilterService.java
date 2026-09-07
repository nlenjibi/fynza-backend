package ecommerce.common.security;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * High-performance Bloom Filter service for O(1) token blacklist checks.
 * 
 * <p>Uses a probabilistic data structure to quickly determine if a token
 * is NOT in the blacklist (definite NO) or might be in the blacklist
 * (MAYBE - requires verification against Caffeine cache).
 * 
 * <p>The Bloom Filter provides:
 * - O(1) lookup performance
 * - Minimal memory footprint
 * - Configurable false positive rate (0.1% default)
 * 
 * <p>Note: A "MAYBE" result always requires verification against the
 * Caffeine tokenBlacklist to avoid blocking valid users.
 * 
 * <p>Optimization: Stats tracking removed for better performance during
 * high-throughput JWT validation.
 */
@Slf4j
@Service
public class BloomFilterService {

    // Guava BloomFilter with 0.1% false positive rate
    private BloomFilter<String> tokenBloomFilter;
    
    // Expected insertions and fpp
    private final long expectedInsertions;
    private final double fpp;
    
    public BloomFilterService(
            @Value("${jwt.bloom-filter.expected-insertions:50000}") long expectedInsertions,
            @Value("${jwt.bloom-filter.fpp:0.001}") double fpp) {
        this.expectedInsertions = expectedInsertions;
        this.fpp = fpp;
        this.tokenBloomFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                expectedInsertions,
                fpp
        );
        
        log.info("BloomFilter initialized with expectedInsertions={}, fpp={}", 
                expectedInsertions, fpp);
    }

    /**
     * Add a token hash to the Bloom Filter.
     * Called when a token is blacklisted.
     * 
     * @param token The token (or hashed token) to add
     */
    public void add(String token) {
        tokenBloomFilter.put(token);
        log.trace("Added token to Bloom Filter: {}...", token.substring(0, 8));
    }

    /**
     * Check if a token might be in the blacklist.
     * 
     * <p>Returns:
     * - FALSE: Token is DEFINITELY NOT in the blacklist (clean rejection)
     * - TRUE: Token MIGHT be in the blacklist (requires Caffeine verification)
     * 
     * @param token The token to check
     * @return false if definitely not blacklisted, true if potentially blacklisted
     */
    public boolean mightContain(String token) {
        return tokenBloomFilter.mightContain(token);
    }

    /**
     * Synchronize the Bloom Filter with the Caffeine blacklist.
     * Rebuilds the Bloom Filter from existing blacklisted tokens.
     * 
     * <p>Note: This should be called during startup or periodically
     * to ensure the Bloom Filter stays in sync with the Caffeine cache.
     * 
     * @param blacklistedTokens Iterable of currently blacklisted token keys
     */
    public void syncWithBlacklist(Iterable<String> blacklistedTokens) {
        BloomFilter<String> newFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                expectedInsertions,
                fpp
        );
        for (String token : blacklistedTokens) {
            newFilter.put(token);
        }
        this.tokenBloomFilter = newFilter;
        log.info("Bloom Filter synchronized with blacklist");
    }
}

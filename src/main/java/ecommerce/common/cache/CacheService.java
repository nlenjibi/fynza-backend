package ecommerce.common.cache;

import java.time.Duration;
import java.util.Optional;

public interface CacheService {

    <T> Optional<T> get(String key, Class<T> type);

    void put(String key, Object value, Duration ttl);

    void evict(String key);

    boolean exists(String key);

    void clear(String keyPattern);
}

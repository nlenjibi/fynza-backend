package ecommerce.common.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Primary Jackson {@link ObjectMapper} used for HTTP request/response serialization.
 *
 * <p>This mapper is intentionally separate from the Redis mapper in {@link RedisConfig}
 * — the HTTP mapper must never carry {@code @class} type metadata in responses.
 */
@Configuration
public class WebConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                // ISO-8601 dates ("2024-01-15T10:30:00Z"), not timestamps
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
                // Omit null fields — reduces payload, prevents leaking internal structure
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                // Forward-compatible deserialization
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                // Integers from JSON number literals round-trip to Long, not Integer
                .enable(DeserializationFeature.USE_LONG_FOR_INTS);
    }
}

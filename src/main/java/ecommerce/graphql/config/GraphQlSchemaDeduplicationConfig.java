package ecommerce.graphql.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.boot.autoconfigure.graphql.GraphQlSourceBuilderCustomizer;
import org.springframework.graphql.execution.GraphQlSource;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Deduplicates GraphQL schema resources before the schema is built.
 *
 * When both src/main/resources/ and target/classes/ are on the classpath
 * (IDE run / mvn spring-boot:run), Spring's PathMatchingResourcePatternResolver
 * finds every .graphqls file twice because the wildcard pattern triggers a
 * ClassLoader.getResources() scan across all classpath entries.
 *
 * This customizer re-resolves the same patterns and keeps only the first
 * occurrence of each relative path under graphql/, replacing whatever the
 * auto-configuration loaded.
 */
@Slf4j
@Configuration
public class GraphQlSchemaDeduplicationConfig {

    @Bean
    public GraphQlSourceBuilderCustomizer schemaDeduplicationCustomizer(
            ResourcePatternResolver resourcePatternResolver,
            GraphQlProperties properties) {
        return builder -> {
            Map<String, Resource> deduplicated = new LinkedHashMap<>();
            for (String location : properties.getSchema().getLocations()) {
                for (String ext : properties.getSchema().getFileExtensions()) {
                    String pattern = location + "*" + ext;
                    try {
                        for (Resource r : resourcePatternResolver.getResources(pattern)) {
                            String uri = r.getURI().toString();
                            int idx = uri.lastIndexOf("/graphql/");
                            String key = idx >= 0 ? uri.substring(idx) : r.getFilename();
                            if (deduplicated.putIfAbsent(key, r) != null) {
                                log.debug("Duplicate schema resource ignored: {}", uri);
                            }
                        }
                    } catch (IOException ignored) {}
                }
            }
            builder.schemaResources(deduplicated.values().toArray(new Resource[0]));
        };
    }
}

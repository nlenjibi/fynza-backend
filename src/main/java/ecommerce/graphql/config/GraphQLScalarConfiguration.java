package ecommerce.graphql.config;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.Value;
import graphql.scalars.ExtendedScalars;
import graphql.schema.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.util.Locale;

/**
 * Registers custom and extended GraphQL scalars for the Fynza schema.
 *
 * <ul>
 *   <li>{@code DateTime} — hand-rolled {@link DateTimeCoercing}; accepts LocalDateTime /
 *       OffsetDateTime / Instant, serialises to UTC ISO-8601 offset string.</li>
 *   <li>{@code Instant}  — hand-rolled {@link InstantCoercing}; serialises to "...Z" UTC string.</li>
 *   <li>{@code BigDecimal}, {@code UUID}, {@code Long} — from graphql-java-extended-scalars.</li>
 *   <li>{@code Upload}   — pass-through for multipart file uploads.</li>
 * </ul>
 */
@Configuration
public class GraphQLScalarConfiguration {

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(dateTimeScalar())
                .scalar(instantScalar())
                .scalar(ExtendedScalars.GraphQLBigDecimal)
                .scalar(ExtendedScalars.UUID)
                .scalar(ExtendedScalars.GraphQLLong)
                .scalar(uploadScalar());
    }

    @Bean
    public GraphQLScalarType dateTimeScalar() {
        return GraphQLScalarType.newScalar()
                .name("DateTime")
                .description("ISO-8601 date-time, normalised to UTC offset on output")
                .coercing(new DateTimeCoercing())
                .build();
    }

    @Bean
    public GraphQLScalarType instantScalar() {
        return GraphQLScalarType.newScalar()
                .name("Instant")
                .description("ISO-8601 UTC instant, serialised as '...Z' string")
                .coercing(new InstantCoercing())
                .build();
    }

    @Bean
    public GraphQLScalarType uploadScalar() {
        return GraphQLScalarType.newScalar()
                .name("Upload")
                .description("Multipart file upload")
                .coercing(new Coercing<Object, Object>() {
                    @Override public Object serialize(Object r, GraphQLContext c, Locale l) { return r; }
                    @Override public Object parseValue(Object i, GraphQLContext c, Locale l) { return i; }
                    @Override public Object parseLiteral(Value<?> i, CoercedVariables v, GraphQLContext c, Locale l) { return i; }
                })
                .build();
    }
}

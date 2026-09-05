package ecommerce.graphql.config;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Locale;

/**
 * Coercing for the {@code Instant} scalar, backed by {@link Instant}.
 *
 * <p>Serialize: {@link Instant} → ISO-8601 UTC string ending in "Z" (e.g. "2026-01-01T00:00:00Z").
 *
 * <p>Parse: accepts both plain instants ("2026-01-01T00:00:00Z") and offset timestamps
 * ("2026-01-01T00:00:00+02:00"), mirroring how the REST layer deserialises {@code Instant}.
 */
public class InstantCoercing implements Coercing<Instant, String> {

    @Override
    public String serialize(Object dataFetcherResult,
                            GraphQLContext graphQLContext,
                            Locale locale) throws CoercingSerializeException {
        if (dataFetcherResult instanceof Instant instant) {
            return instant.toString();
        }
        throw new CoercingSerializeException(
                "Expected Instant, got: " + dataFetcherResult.getClass().getName());
    }

    @Override
    public Instant parseValue(Object input,
                              GraphQLContext graphQLContext,
                              Locale locale) throws CoercingParseValueException {
        try {
            return parse(input.toString());
        } catch (Exception e) {
            throw new CoercingParseValueException("Cannot parse Instant value: " + input, e);
        }
    }

    @Override
    public Instant parseLiteral(Value<?> input,
                                CoercedVariables variables,
                                GraphQLContext graphQLContext,
                                Locale locale) throws CoercingParseLiteralException {
        if (input instanceof StringValue sv) {
            try {
                return parse(sv.getValue());
            } catch (Exception e) {
                throw new CoercingParseLiteralException(
                        "Cannot parse Instant literal: " + sv.getValue(), e);
            }
        }
        throw new CoercingParseLiteralException("Expected StringValue for Instant scalar");
    }

    private Instant parse(String value) {
        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            return OffsetDateTime.parse(value).toInstant();
        }
    }
}

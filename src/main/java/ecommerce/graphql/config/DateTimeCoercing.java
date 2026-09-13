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
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Coercing for the {@code DateTime} scalar.
 *
 * <p>Serialize: {@link LocalDateTime}, {@link OffsetDateTime}, or {@link Instant} → ISO-8601
 * offset string normalised to UTC ("...+00:00").
 *
 * <p>Parse: three-tier strategy — full ISO with offset → local with seconds → local without
 * seconds (e.g. "2026-08-25T14:00") — so API clients omitting the offset or seconds still work.
 */
public class DateTimeCoercing implements Coercing<OffsetDateTime, String> {

    private static final DateTimeFormatter ISO_OFFSET    = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final DateTimeFormatter NO_SECONDS    = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Override
    public String serialize(Object dataFetcherResult,
                            GraphQLContext graphQLContext,
                            Locale locale) throws CoercingSerializeException {
        if (dataFetcherResult == null) return null;
        if (dataFetcherResult instanceof LocalDateTime ldt)  return ldt.atOffset(ZoneOffset.UTC).format(ISO_OFFSET);
        if (dataFetcherResult instanceof OffsetDateTime odt) return odt.withOffsetSameInstant(ZoneOffset.UTC).format(ISO_OFFSET);
        if (dataFetcherResult instanceof Instant instant)    return instant.atOffset(ZoneOffset.UTC).format(ISO_OFFSET);
        if (dataFetcherResult instanceof String s)           return s;
        throw new CoercingSerializeException(
                "Expected LocalDateTime, OffsetDateTime, or Instant, got: "
                + dataFetcherResult.getClass().getName());
    }

    @Override
    public OffsetDateTime parseValue(Object input,
                                     GraphQLContext graphQLContext,
                                     Locale locale) throws CoercingParseValueException {
        try {
            return parse(input.toString());
        } catch (Exception e) {
            throw new CoercingParseValueException("Cannot parse DateTime value: " + input, e);
        }
    }

    @Override
    public OffsetDateTime parseLiteral(Value<?> input,
                                       CoercedVariables variables,
                                       GraphQLContext graphQLContext,
                                       Locale locale) throws CoercingParseLiteralException {
        if (input instanceof StringValue sv) {
            try {
                return parse(sv.getValue());
            } catch (Exception e) {
                throw new CoercingParseLiteralException(
                        "Cannot parse DateTime literal: " + sv.getValue(), e);
            }
        }
        throw new CoercingParseLiteralException("Expected StringValue for DateTime scalar");
    }

    private OffsetDateTime parse(String value) {
        // 1. Full ISO-8601 with offset: "2026-08-25T14:00:00+00:00"
        try { return OffsetDateTime.parse(value, ISO_OFFSET); } catch (Exception ignored) { }
        // 2. Local with seconds, no offset: "2026-08-25T14:00:00"
        try { return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME).atOffset(ZoneOffset.UTC); } catch (Exception ignored) { }
        // 3. Local without seconds: "2026-08-25T14:00"
        return LocalDateTime.parse(value, NO_SECONDS).atOffset(ZoneOffset.UTC);
    }
}

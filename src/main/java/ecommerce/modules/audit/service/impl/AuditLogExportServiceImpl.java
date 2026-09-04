package ecommerce.modules.audit.service.impl;

import ecommerce.modules.audit.dto.AuditLogExportRow;
import ecommerce.modules.audit.dto.AuditLogFilter;
import ecommerce.modules.audit.entity.AuditLog;
import ecommerce.modules.audit.service.AuditLogExportService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Streams audit log entries as CSV using keyset (seek) pagination in pages of
 * {@value #PAGE_SIZE} rows — no COUNT query, no offset, no full memory load.
 * Defaults to the last 90 days when no start date is provided to prevent full-table scans.
 */
@Service
@RequiredArgsConstructor
public class AuditLogExportServiceImpl implements AuditLogExportService {

    private static final int      PAGE_SIZE          = 500;
    private static final int      DEFAULT_DAYS       = 90;
    private static final String   FIELD_OCCURRED_AT  = "occurredAt";
    private static final String   FIELD_ID           = "id";
    private static final String[] CSV_HEADERS = {
        "ID", "Occurred At", "Actor Email", "Actor Role",
        "Action", "Entity Type", "IP Address", "Reason", "Status"
    };

    private final EntityManager entityManager;

    @Override
    public StreamingResponseBody exportCsv(AuditLogFilter filter) {
        if (filter.getFrom() == null) {
            filter.setFrom(Instant.now().minus(DEFAULT_DAYS, ChronoUnit.DAYS));
        }

        Specification<AuditLog> spec = buildSpec(filter);

        return outputStream -> {
            Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
            CSVFormat format = CSVFormat.DEFAULT.builder().setHeader(CSV_HEADERS).get();

            try (CSVPrinter printer = new CSVPrinter(writer, format)) {
                Instant cursorOccurredAt = null;
                UUID    cursorId         = null;
                List<AuditLogExportRow> batch;

                do {
                    batch = fetchPage(spec, cursorOccurredAt, cursorId);
                    writeBatch(batch, printer);
                    if (!batch.isEmpty()) {
                        AuditLogExportRow last = batch.get(batch.size() - 1);
                        cursorOccurredAt = last.occurredAt();
                        cursorId         = last.id();
                    }
                } while (batch.size() == PAGE_SIZE);
            }
        };
    }

    /**
     * Fetches one keyset page using a narrow {@link AuditLogExportRow} projection —
     * skips the JSONB state columns entirely. Ordered {@code occurredAt DESC, id DESC}.
     */
    private List<AuditLogExportRow> fetchPage(Specification<AuditLog> spec,
                                              Instant cursorOccurredAt, UUID cursorId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<AuditLogExportRow> cq = cb.createQuery(AuditLogExportRow.class);
        Root<AuditLog> root = cq.from(AuditLog.class);

        List<Predicate> predicates = new ArrayList<>();
        Predicate filterPredicate = spec.toPredicate(root, cq, cb);
        if (filterPredicate != null) predicates.add(filterPredicate);

        if (cursorOccurredAt != null) {
            predicates.add(cb.or(
                    cb.lessThan(root.get(FIELD_OCCURRED_AT), cursorOccurredAt),
                    cb.and(
                            cb.equal(root.get(FIELD_OCCURRED_AT), cursorOccurredAt),
                            cb.lessThan(root.<UUID>get(FIELD_ID).as(String.class),
                                        cursorId.toString())
                    )
            ));
        }

        cq.select(cb.construct(AuditLogExportRow.class,
                root.get(FIELD_ID),
                root.get(FIELD_OCCURRED_AT),
                root.get("actorEmail"),
                root.get("actorRole").as(String.class),
                root.get("action"),
                root.get("entityType"),
                root.get("ipAddress"),
                root.get("reason"),
                root.get("status").as(String.class)));
        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(root.get(FIELD_OCCURRED_AT)), cb.desc(root.get(FIELD_ID)));

        return entityManager.createQuery(cq)
                .setMaxResults(PAGE_SIZE)
                .getResultList();
    }

    private void writeBatch(List<AuditLogExportRow> batch, CSVPrinter printer) throws IOException {
        for (AuditLogExportRow row : batch) {
            printer.printRecord(
                    row.id(),
                    row.occurredAt(),
                    row.actorEmail(),
                    row.actorRole(),
                    row.action(),
                    row.entityType(),
                    row.ipAddress(),
                    row.reason(),
                    row.status());
        }
        printer.flush();
    }

    // ── specification builder ────────────────────────────────────────────────

    private Specification<AuditLog> buildSpec(AuditLogFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.getActorPublicId() != null)
                predicates.add(cb.equal(root.get("actorPublicId"), filter.getActorPublicId()));
            if (filter.getEntityType() != null)
                predicates.add(cb.equal(root.get("entityType"), filter.getEntityType()));
            if (filter.getEntityPublicId() != null)
                predicates.add(cb.equal(root.get("entityPublicId"), filter.getEntityPublicId()));
            if (filter.getActions() != null && !filter.getActions().isEmpty())
                predicates.add(root.get("action").in(filter.getActions()));
            if (filter.getFrom() != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get(FIELD_OCCURRED_AT), filter.getFrom()));
            if (filter.getTo() != null)
                predicates.add(cb.lessThanOrEqualTo(root.get(FIELD_OCCURRED_AT), filter.getTo()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

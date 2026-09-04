package com.aoms.aomsbackend.attendance.repository;

import com.aoms.aomsbackend.attendance.dto.NoShowReportRecordDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public class NoShowReadModelRepositoryImpl implements NoShowReadModelRepositoryCustom {

    private static final String COUNT_SUBQUERY = """
            (SELECT COUNT(*) FROM no_show_record_read_model c
             WHERE c.user_id = n.user_id
               AND c.booking_date BETWEEN :fromDate AND :toDate
               %s)
            """;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<NoShowReportRecordDto> findReportPage(
            UUID organisationId,
            LocalDate fromDate,
            LocalDate toDate,
            UUID employeeId,
            String department,
            Pageable pageable) {

        String orgFilter        = buildOrgFilter(organisationId);
        String orgFilterInCount = buildOrgFilterForCount(organisationId);
        String employeeFilter   = employeeId != null ? "AND n.user_id = :employeeId" : "";
        String departmentFilter = department != null ? "AND u.department = :department" : "";

        String countSubq = COUNT_SUBQUERY.formatted(orgFilterInCount);

        String dataSql = """
                SELECT n.no_show_record_id, n.user_id,
                       u.first_name || ' ' || u.last_name AS employee_name,
                       u.department,
                       n.booking_date, n.seat_reference, n.auto_released_at,
                       %s AS no_show_count_in_period
                FROM no_show_record_read_model n
                JOIN users u ON u.id = n.user_id
                WHERE n.booking_date BETWEEN :fromDate AND :toDate
                %s %s %s
                ORDER BY n.booking_date DESC
                """.formatted(countSubq, orgFilter, employeeFilter, departmentFilter);

        String countSql = """
                SELECT COUNT(*)
                FROM no_show_record_read_model n
                JOIN users u ON u.id = n.user_id
                WHERE n.booking_date BETWEEN :fromDate AND :toDate
                %s %s %s
                """.formatted(orgFilter, employeeFilter, departmentFilter);

        var dataQuery = entityManager.createNativeQuery(dataSql)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize());

        var countQuery = entityManager.createNativeQuery(countSql)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate);

        applyOptionalParams(dataQuery, countQuery, organisationId, employeeId, department);

        long total = ((Number) countQuery.getSingleResult()).longValue();

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQuery.getResultList();
        List<NoShowReportRecordDto> records = rows.stream()
                .map(this::toDto)
                .toList();

        return new PageImpl<>(records, pageable, total);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private String buildOrgFilter(UUID organisationId) {
        return organisationId != null ? "AND n.organisation_id = :organisationId" : "";
    }

    private String buildOrgFilterForCount(UUID organisationId) {
        return organisationId != null ? "AND c.organisation_id = :organisationId" : "";
    }

    private void applyOptionalParams(
            jakarta.persistence.Query dataQuery,
            jakarta.persistence.Query countQuery,
            UUID organisationId, UUID employeeId, String department) {

        if (organisationId != null) {
            dataQuery.setParameter("organisationId", organisationId);
            countQuery.setParameter("organisationId", organisationId);
        }
        if (employeeId != null) {
            dataQuery.setParameter("employeeId", employeeId);
            countQuery.setParameter("employeeId", employeeId);
        }
        if (department != null) {
            dataQuery.setParameter("department", department);
            countQuery.setParameter("department", department);
        }
    }

    private NoShowReportRecordDto toDto(Object[] row) {
        return NoShowReportRecordDto.builder()
                .noShowRecordId(toUuid(row[0]))
                .employeeId(toUuid(row[1]))
                .employeeName((String) row[2])
                .department((String) row[3])
                .bookingDate(toLocalDate(row[4]))
                .seatReference((String) row[5])
                .autoReleasedAt(toInstant(row[6]))
                .noShowCountInPeriod(((Number) row[7]).intValue())
                .build();
    }

    private UUID toUuid(Object value) {
        if (value == null) return null;
        return value instanceof UUID u ? u : UUID.fromString(value.toString());
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate ld) return ld;
        if (value instanceof java.sql.Date d) return d.toLocalDate();
        return LocalDate.parse(value.toString());
    }

    private Instant toInstant(Object value) {
        if (value == null) return null;
        if (value instanceof Instant i) return i;
        if (value instanceof java.sql.Timestamp ts) return ts.toInstant();
        if (value instanceof java.time.OffsetDateTime odt) return odt.toInstant();
        return Instant.parse(value.toString());
    }
}

package ecommerce.modules.audit.service.impl;

import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.audit.constant.AuditStatus;
import ecommerce.modules.audit.dto.AuditActorInfo;
import ecommerce.modules.audit.dto.AuditLogDetailResponse;
import ecommerce.modules.audit.dto.AuditLogFilter;
import ecommerce.modules.audit.dto.AuditLogResponse;
import ecommerce.modules.audit.entity.AuditLog;
import ecommerce.modules.audit.repository.AuditLogRepository;
import ecommerce.modules.audit.service.AuditLogQueryService;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AuditLogQueryServiceImpl implements AuditLogQueryService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> query(AuditLogFilter filter, Pageable pageable) {
        return auditLogRepository.findAll(buildSpec(filter), pageable)
                .map(this::toListResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogDetailResponse getById(UUID logId) {
        AuditLog log = auditLogRepository.findById(logId)
                .orElseThrow(() -> new ResourceNotFoundException("Audit log not found: " + logId));
        return toDetailResponse(log);
    }

    // ── specification builder ────────────────────────────────────────────────

    private Specification<AuditLog> buildSpec(AuditLogFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            applyFilters(predicates, filter, root, cb);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void applyFilters(List<Predicate> predicates, AuditLogFilter filter,
                               Root<AuditLog> root, CriteriaBuilder cb) {
        if (filter.getActorPublicId() != null)
            predicates.add(cb.equal(root.get("actorPublicId"), filter.getActorPublicId()));
        if (filter.getEntityType() != null)
            predicates.add(cb.equal(root.get("entityType"), filter.getEntityType()));
        if (filter.getEntityPublicId() != null)
            predicates.add(cb.equal(root.get("entityPublicId"), filter.getEntityPublicId()));
        if (filter.getActions() != null && !filter.getActions().isEmpty())
            predicates.add(root.get("action").in(filter.getActions()));
        if (filter.getStatus() != null) {
            AuditStatus parsed = AuditStatus.from(filter.getStatus());
            if (parsed != null) predicates.add(cb.equal(root.get("status"), parsed));
        }
        if (filter.getFrom() != null)
            predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), filter.getFrom()));
        if (filter.getTo() != null)
            predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), filter.getTo()));
    }

    // ── mapping ──────────────────────────────────────────────────────────────

    private AuditLogResponse toListResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .actorPublicId(log.getActorPublicId())
                .actor(AuditActorInfo.builder()
                        .email(log.getActorEmail())
                        .build())
                .actorRole(log.getActorRole())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityPublicId(log.getEntityPublicId())
                .ipAddress(log.getIpAddress())
                .occurredAt(log.getOccurredAt())
                .correlationId(log.getCorrelationId())
                .status(log.getStatus() != null ? log.getStatus().name() : null)
                .build();
    }

    private AuditLogDetailResponse toDetailResponse(AuditLog log) {
        Map<String, Object> prev = log.getPreviousState();
        Map<String, Object> next = log.getNewState();

        return AuditLogDetailResponse.builder()
                .id(log.getId())
                .actorPublicId(log.getActorPublicId())
                .actor(AuditActorInfo.builder()
                        .email(log.getActorEmail())
                        .build())
                .actorRole(log.getActorRole())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityPublicId(log.getEntityPublicId())
                .previousState(prev)
                .newState(next)
                .diff(computeDiff(prev, next))
                .reason(log.getReason())
                .ipAddress(log.getIpAddress())
                .occurredAt(log.getOccurredAt())
                .correlationId(log.getCorrelationId())
                .status(log.getStatus() != null ? log.getStatus().name() : null)
                .build();
    }

    private Map<String, AuditLogDetailResponse.FieldDiff> computeDiff(
            Map<String, Object> prev, Map<String, Object> next) {
        if (prev == null && next == null) return Map.of();

        Map<String, Object> before = prev != null ? prev : Map.of();
        Map<String, Object> after  = next != null ? next : Map.of();

        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(before.keySet());
        allKeys.addAll(after.keySet());

        Map<String, AuditLogDetailResponse.FieldDiff> diff = new LinkedHashMap<>();
        for (String key : allKeys) {
            Object b = before.get(key);
            Object a = after.get(key);
            if (!Objects.equals(b, a)) {
                diff.put(key, AuditLogDetailResponse.FieldDiff.builder()
                        .before(b).after(a).build());
            }
        }
        return diff;
    }
}

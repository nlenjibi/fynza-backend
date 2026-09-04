package com.aoms.aomsbackend.audit.service.impl;

import com.aoms.aomsbackend.audit.dto.AuditLogFilter;
import com.aoms.aomsbackend.audit.dto.AuditLogResponse;
import com.aoms.aomsbackend.audit.entity.AuditLog;
import com.aoms.aomsbackend.audit.repository.AuditLogRepository;
import com.aoms.aomsbackend.audit.service.AuditLogQueryService;
import com.aoms.aomsbackend.auth.entity.UserRole;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.auth.service.UserRoleLookupService;
import com.aoms.aomsbackend.common.exception.ForbiddenException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogQueryServiceImpl implements AuditLogQueryService {

    private static final Set<UserRoleType> ALLOWED_ROLES =
            Set.of(UserRoleType.FACILITIES_ADMIN, UserRoleType.HR, UserRoleType.SUPER_ADMIN);

    private final AuditLogRepository auditLogRepository;
    private final UserRoleLookupService userRoleLookupService;

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> query(AuditLogFilter filter, Pageable pageable,
                                        UUID actorId, UUID locationId) {
        validateAccess(actorId, locationId);

        boolean isSuperAdmin = isSuperAdmin(actorId);
        Specification<AuditLog> spec = buildSpec(filter, isSuperAdmin ? null : locationId);

        return auditLogRepository.findAll(spec, pageable).map(this::toResponse);
    }

    // ── access guard ─────────────────────────────────────────────────────────────

    private void validateAccess(UUID actorId, UUID locationId) {
        boolean hasAllowedRole = userRoleLookupService.getRolesForUser(actorId).stream()
                .filter(r -> r.getOrganisationId() == null || r.getOrganisationId().equals(locationId))
                .map(UserRole::getRole)
                .anyMatch(ALLOWED_ROLES::contains);

        if (!hasAllowedRole) {
            throw new ForbiddenException();
        }
    }

    private boolean isSuperAdmin(UUID actorId) {
        return userRoleLookupService.getRolesForUser(actorId).stream()
                .map(UserRole::getRole)
                .anyMatch(r -> r == UserRoleType.SUPER_ADMIN);
    }

    // ── specification builder ─────────────────────────────────────────────────────

    private Specification<AuditLog> buildSpec(AuditLogFilter filter, UUID scopedLocationId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (scopedLocationId != null) {
                predicates.add(cb.equal(root.get("locationId"), scopedLocationId));
            }
            if (filter.getActorId() != null) {
                predicates.add(cb.equal(root.get("actorId"), filter.getActorId()));
            }
            if (filter.getEntityType() != null) {
                predicates.add(cb.equal(root.get("entityType"), filter.getEntityType()));
            }
            if (filter.getEntityId() != null) {
                predicates.add(cb.equal(root.get("entityId"), filter.getEntityId()));
            }
            if (filter.getLocationId() != null) {
                predicates.add(cb.equal(root.get("locationId"), filter.getLocationId()));
            }
            if (filter.getAction() != null) {
                predicates.add(cb.equal(root.get("action"), filter.getAction()));
            }
            if (filter.getFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), filter.getFrom()));
            }
            if (filter.getTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), filter.getTo()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // ── mapping ───────────────────────────────────────────────────────────────────

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .actorId(log.getActorId())
                .actorRole(log.getActorRole())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .locationId(log.getLocationId())
                .previousState(log.getPreviousState())
                .newState(log.getNewState())
                .occurredAt(log.getOccurredAt())
                .correlationId(log.getCorrelationId())
                .build();
    }
}

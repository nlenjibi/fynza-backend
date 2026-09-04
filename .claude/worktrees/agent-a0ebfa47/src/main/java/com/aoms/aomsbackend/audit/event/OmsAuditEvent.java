package com.aoms.aomsbackend.audit.event;

import com.aoms.aomsbackend.auth.entity.UserRoleType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record OmsAuditEvent(
        UUID eventId,
        UUID actorId,
        UserRoleType actorRole,
        String action,
        String entityType,
        UUID entityId,
        UUID locationId,
        Map<String, Object> previousState,
        Map<String, Object> newState,
        UUID correlationId,
        Instant occurredAt
) {}

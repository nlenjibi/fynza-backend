package com.aoms.aomsbackend.audit.dto;

import com.aoms.aomsbackend.auth.entity.UserRoleType;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class AuditLogEntry {
    private final UUID actorId;
    private final UserRoleType actorRole;
    private final String action;
    private final String entityType;
    private final UUID entityId;
    private final UUID locationId;
    private final Map<String, Object> previousState;
    private final Map<String, Object> newState;
    private final UUID correlationId;
}

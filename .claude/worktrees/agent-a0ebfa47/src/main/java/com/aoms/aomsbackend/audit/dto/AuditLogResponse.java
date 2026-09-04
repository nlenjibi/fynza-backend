package com.aoms.aomsbackend.audit.dto;

import com.aoms.aomsbackend.auth.entity.UserRoleType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class AuditLogResponse {
    private UUID id;
    private UUID actorId;
    private UserRoleType actorRole;
    private String action;
    private String entityType;
    private UUID entityId;
    private UUID locationId;
    private Map<String, Object> previousState;
    private Map<String, Object> newState;
    private Instant occurredAt;
    private UUID correlationId;
}

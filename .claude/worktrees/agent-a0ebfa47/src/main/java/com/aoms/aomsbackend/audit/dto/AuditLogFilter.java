package com.aoms.aomsbackend.audit.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class AuditLogFilter {
    private UUID actorId;
    private String entityType;
    private UUID entityId;
    private UUID locationId;
    private String action;
    private Instant from;
    private Instant to;
}

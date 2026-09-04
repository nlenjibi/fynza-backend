package com.aoms.aomsbackend.audit.service;

import com.aoms.aomsbackend.audit.dto.AuditLogFilter;
import com.aoms.aomsbackend.audit.dto.AuditLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AuditLogQueryService {
    Page<AuditLogResponse> query(AuditLogFilter filter, Pageable pageable, UUID actorId, UUID locationId);
}

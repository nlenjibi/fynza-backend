package com.aoms.aomsbackend.audit.service;

import com.aoms.aomsbackend.audit.dto.AuditLogEntry;

public interface AuditLogService {
    void log(AuditLogEntry entry);
}

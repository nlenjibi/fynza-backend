package com.aoms.aomsbackend.audit.event;

import com.aoms.aomsbackend.audit.repository.AuditLogRepository;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditEventListenerTest {

    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks private AuditEventListener listener;

    @Test
    void onEvent_persistsAuditLog() {
        listener.onAuditEvent(buildEvent(UUID.randomUUID()));

        verify(auditLogRepository).save(any());
    }

    private OmsAuditEvent buildEvent(UUID eventId) {
        return new OmsAuditEvent(
                eventId,
                UUID.randomUUID(),
                UserRoleType.FACILITIES_ADMIN,
                "SEAT_BOOKED",
                "SeatBooking",
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                null,
                Instant.now()
        );
    }
}

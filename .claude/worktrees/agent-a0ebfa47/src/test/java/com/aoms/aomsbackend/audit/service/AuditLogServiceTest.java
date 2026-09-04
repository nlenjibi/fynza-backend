package com.aoms.aomsbackend.audit.service;

import com.aoms.aomsbackend.audit.dto.AuditLogEntry;
import com.aoms.aomsbackend.audit.event.OmsAuditEvent;
import com.aoms.aomsbackend.audit.service.impl.AuditLogServiceImpl;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AuditLogServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuditLogServiceImpl(eventPublisher);
    }

    @Test
    void log_publishesApplicationEventWithCorrectPayload() {
        UUID actorId   = UUID.randomUUID();
        UUID entityId  = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();

        AuditLogEntry entry = AuditLogEntry.builder()
                .actorId(actorId)
                .actorRole(UserRoleType.FACILITIES_ADMIN)
                .action("SEAT_BOOKED")
                .entityType("SeatBooking")
                .entityId(entityId)
                .locationId(locationId)
                .build();

        service.log(entry);

        ArgumentCaptor<OmsAuditEvent> captor = ArgumentCaptor.forClass(OmsAuditEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        OmsAuditEvent published = captor.getValue();
        assertThat(published.actorId()).isEqualTo(actorId);
        assertThat(published.actorRole()).isEqualTo(UserRoleType.FACILITIES_ADMIN);
        assertThat(published.action()).isEqualTo("SEAT_BOOKED");
        assertThat(published.entityType()).isEqualTo("SeatBooking");
        assertThat(published.entityId()).isEqualTo(entityId);
        assertThat(published.locationId()).isEqualTo(locationId);
        assertThat(published.eventId()).isNotNull();
        assertThat(published.occurredAt()).isNotNull();
    }

    @Test
    void log_generatesUniqueEventIdPerCall() {
        AuditLogEntry entry = AuditLogEntry.builder()
                .actorId(UUID.randomUUID())
                .actorRole(UserRoleType.EMPLOYEE)
                .action("TEST_ACTION")
                .entityType("Test")
                .entityId(UUID.randomUUID())
                .build();

        service.log(entry);
        service.log(entry);

        ArgumentCaptor<OmsAuditEvent> captor = ArgumentCaptor.forClass(OmsAuditEvent.class);
        verify(eventPublisher, times(2)).publishEvent(captor.capture());

        List<OmsAuditEvent> events = captor.getAllValues();
        assertThat(events.get(0).eventId()).isNotEqualTo(events.get(1).eventId());
    }
}

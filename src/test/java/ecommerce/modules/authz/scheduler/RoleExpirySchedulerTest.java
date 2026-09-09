package ecommerce.modules.authz.scheduler;

import ecommerce.common.util.TokenValidationService;
import ecommerce.modules.audit.constant.AuditAction;
import ecommerce.modules.audit.dto.AuditLogEntry;
import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.authz.entity.RoleEntity;
import ecommerce.modules.authz.entity.UserRoleEntity;
import ecommerce.modules.authz.repository.UserRoleEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleExpiryScheduler Tests")
class RoleExpirySchedulerTest {

    @Mock
    private UserRoleEntityRepository userRoleRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private TokenValidationService tokenValidationService;

    @InjectMocks
    private RoleExpiryScheduler roleExpiryScheduler;

    private UUID userId1;
    private UUID userId2;
    private RoleEntity role;

    @BeforeEach
    void setUp() {
        userId1 = UUID.randomUUID();
        userId2 = UUID.randomUUID();

        role = RoleEntity.builder()
                .id(UUID.randomUUID())
                .code("TEMP_SELLER")
                .displayName("Temporary Seller")
                .active(true)
                .build();
    }

    // ── No expired assignments ─────────────────────────────────────────────────

    @Nested
    @DisplayName("When no expired assignments exist")
    class NoExpiredAssignmentsTests {

        @Test
        @DisplayName("Should return early without saving, evicting, or auditing")
        void expireTemporaryRoles_WhenNoExpired_ReturnsEarly() {
            when(userRoleRepository.findExpiredActiveAssignments(any(Instant.class)))
                    .thenReturn(List.of());

            roleExpiryScheduler.expireTemporaryRoles();

            verify(userRoleRepository, never()).save(any());
            verify(tokenValidationService, never()).evictPrincipal(any());
            verify(auditLogService, never()).logImmediately(any());
        }
    }

    // ── Single expired assignment ──────────────────────────────────────────────

    @Nested
    @DisplayName("When one expired assignment exists")
    class SingleExpiredAssignmentTests {

        @Test
        @DisplayName("Should set active=false, save, evict principal, and log TEMPORARY_ACCESS_EXPIRED")
        void expireTemporaryRoles_WithOneExpired_ProcessesCorrectly() {
            UserRoleEntity expiredAssignment = UserRoleEntity.builder()
                    .id(UUID.randomUUID())
                    .userId(userId1)
                    .role(role)
                    .expiresAt(Instant.now().minusSeconds(60))
                    .active(true)
                    .build();

            when(userRoleRepository.findExpiredActiveAssignments(any(Instant.class)))
                    .thenReturn(List.of(expiredAssignment));
            when(userRoleRepository.save(expiredAssignment)).thenReturn(expiredAssignment);

            roleExpiryScheduler.expireTemporaryRoles();

            assertThat(expiredAssignment.isActive()).isFalse();
            verify(userRoleRepository).save(expiredAssignment);
            verify(tokenValidationService).evictPrincipal(userId1);

            ArgumentCaptor<AuditLogEntry> auditCaptor = ArgumentCaptor.forClass(AuditLogEntry.class);
            verify(auditLogService).logImmediately(auditCaptor.capture());
            AuditLogEntry entry = auditCaptor.getValue();
            assertThat(entry.getAction()).isEqualTo(AuditAction.TEMPORARY_ACCESS_EXPIRED);
            assertThat(entry.getEntityType()).isEqualTo("USER");
            assertThat(entry.getEntityPublicId()).isEqualTo(userId1);
            assertThat(entry.getReason()).contains("TEMP_SELLER");
            assertThat(entry.getStatus()).isEqualTo(AuditLogEntry.STATUS_SUCCESS);
        }
    }

    // ── Multiple expired assignments ───────────────────────────────────────────

    @Nested
    @DisplayName("When multiple expired assignments exist")
    class MultipleExpiredAssignmentsTests {

        @Test
        @DisplayName("Should process each assignment independently")
        void expireTemporaryRoles_WithMultipleExpired_ProcessesEachIndependently() {
            RoleEntity role2 = RoleEntity.builder()
                    .id(UUID.randomUUID())
                    .code("TEMP_MODERATOR")
                    .displayName("Temporary Moderator")
                    .active(true)
                    .build();

            UserRoleEntity expired1 = UserRoleEntity.builder()
                    .id(UUID.randomUUID())
                    .userId(userId1)
                    .role(role)
                    .expiresAt(Instant.now().minusSeconds(120))
                    .active(true)
                    .build();

            UserRoleEntity expired2 = UserRoleEntity.builder()
                    .id(UUID.randomUUID())
                    .userId(userId2)
                    .role(role2)
                    .expiresAt(Instant.now().minusSeconds(60))
                    .active(true)
                    .build();

            when(userRoleRepository.findExpiredActiveAssignments(any(Instant.class)))
                    .thenReturn(List.of(expired1, expired2));
            when(userRoleRepository.save(any(UserRoleEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            roleExpiryScheduler.expireTemporaryRoles();

            assertThat(expired1.isActive()).isFalse();
            assertThat(expired2.isActive()).isFalse();

            verify(userRoleRepository, times(2)).save(any(UserRoleEntity.class));
            verify(tokenValidationService).evictPrincipal(userId1);
            verify(tokenValidationService).evictPrincipal(userId2);

            ArgumentCaptor<AuditLogEntry> auditCaptor = ArgumentCaptor.forClass(AuditLogEntry.class);
            verify(auditLogService, times(2)).logImmediately(auditCaptor.capture());

            List<AuditLogEntry> entries = auditCaptor.getAllValues();
            assertThat(entries).allMatch(e -> e.getAction().equals(AuditAction.TEMPORARY_ACCESS_EXPIRED));
            assertThat(entries).allMatch(e -> e.getEntityType().equals("USER"));

            List<UUID> auditedUsers = entries.stream().map(AuditLogEntry::getEntityPublicId).toList();
            assertThat(auditedUsers).containsExactlyInAnyOrder(userId1, userId2);
        }

        @Test
        @DisplayName("Should log correct role code for each expired assignment")
        void expireTemporaryRoles_WithMultipleExpired_LogsCorrectRoleCode() {
            UserRoleEntity expired1 = UserRoleEntity.builder()
                    .id(UUID.randomUUID())
                    .userId(userId1)
                    .role(role)
                    .expiresAt(Instant.now().minusSeconds(120))
                    .active(true)
                    .build();

            when(userRoleRepository.findExpiredActiveAssignments(any(Instant.class)))
                    .thenReturn(List.of(expired1));
            when(userRoleRepository.save(any(UserRoleEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            roleExpiryScheduler.expireTemporaryRoles();

            ArgumentCaptor<AuditLogEntry> auditCaptor = ArgumentCaptor.forClass(AuditLogEntry.class);
            verify(auditLogService).logImmediately(auditCaptor.capture());
            assertThat(auditCaptor.getValue().getReason()).contains("TEMP_SELLER");
        }
    }
}

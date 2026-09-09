package ecommerce.modules.seller.service;

import ecommerce.common.enums.SellerStatus;
import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.seller.dto.request.SellerStatusRequest;
import ecommerce.modules.seller.dto.response.SellerResponse;
import ecommerce.modules.seller.entity.Seller;
import ecommerce.modules.seller.entity.SellerStatusHistory;
import ecommerce.modules.seller.exception.SellerNotFoundException;
import ecommerce.modules.seller.exception.SellerStatusTransitionException;
import ecommerce.modules.seller.mapper.SellerMapper;
import ecommerce.modules.seller.repository.SellerRepository;
import ecommerce.modules.seller.repository.SellerStatusHistoryRepository;
import ecommerce.modules.seller.service.impl.SellerStatusServiceImpl;
import ecommerce.modules.seller.validation.SellerStatusTransitionValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SellerStatusServiceImpl Tests")
class SellerStatusServiceImplTest {

    @Mock private SellerRepository                sellerRepository;
    @Mock private SellerStatusHistoryRepository   historyRepository;
    @Mock private SellerMapper                    mapper;
    @Mock private AuditLogService                 auditLogService;
    @Mock private SellerStatusTransitionValidator transitionValidator;

    @InjectMocks
    private SellerStatusServiceImpl service;

    private UUID sellerPublicId;
    private UUID actorId;
    private Seller seller;

    @BeforeEach
    void setUp() {
        sellerPublicId = UUID.randomUUID();
        actorId        = UUID.randomUUID();

        seller = Seller.builder()
                .sellerNumber("SEL-000001")
                .status(SellerStatus.ACTIVE)
                .build();
        setPublicId(seller, sellerPublicId);

        when(sellerRepository.findByPublicId(sellerPublicId)).thenReturn(Optional.of(seller));
        when(sellerRepository.save(any(Seller.class))).thenReturn(seller);
        when(historyRepository.save(any(SellerStatusHistory.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(Seller.class))).thenReturn(SellerResponse.builder().build());
    }

    // ── suspendSeller() ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("suspendSeller()")
    class SuspendSeller {

        @Test
        @DisplayName("Happy path — changes status to SUSPENDED and records history")
        void suspendSeller_happyPath() {
            SellerStatusRequest request = new SellerStatusRequest();
            request.setReason("Policy violation");
            request.setExpiresAt(Instant.now().plusSeconds(3600));

            service.suspendSeller(sellerPublicId, actorId, request);

            assertThat(seller.getStatus()).isEqualTo(SellerStatus.SUSPENDED);
            verify(transitionValidator).validate(SellerStatus.ACTIVE, SellerStatus.SUSPENDED);
            verify(sellerRepository).save(seller);

            ArgumentCaptor<SellerStatusHistory> captor = ArgumentCaptor.forClass(SellerStatusHistory.class);
            verify(historyRepository).save(captor.capture());
            SellerStatusHistory history = captor.getValue();
            assertThat(history.getPreviousStatus()).isEqualTo(SellerStatus.ACTIVE);
            assertThat(history.getNewStatus()).isEqualTo(SellerStatus.SUSPENDED);
            assertThat(history.getReason()).isEqualTo("Policy violation");
            assertThat(history.getChangedBy()).isEqualTo(actorId);
            assertThat(history.getExpiresAt()).isNotNull();

            verify(auditLogService).log(any());
        }

        @Test
        @DisplayName("Throws SellerNotFoundException when seller not found")
        void suspendSeller_notFound_throws() {
            UUID unknownId = UUID.randomUUID();
            when(sellerRepository.findByPublicId(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.suspendSeller(unknownId, actorId, new SellerStatusRequest()))
                    .isInstanceOf(SellerNotFoundException.class);
        }

        @Test
        @DisplayName("Throws SellerStatusTransitionException on invalid transition")
        void suspendSeller_invalidTransition_throws() {
            seller.setStatus(SellerStatus.BLOCKED);
            doThrow(new SellerStatusTransitionException(SellerStatus.BLOCKED, SellerStatus.SUSPENDED))
                    .when(transitionValidator).validate(SellerStatus.BLOCKED, SellerStatus.SUSPENDED);

            assertThatThrownBy(() -> service.suspendSeller(sellerPublicId, actorId, new SellerStatusRequest()))
                    .isInstanceOf(SellerStatusTransitionException.class)
                    .hasMessageContaining("BLOCKED")
                    .hasMessageContaining("SUSPENDED");
        }
    }

    // ── activateSeller() ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("activateSeller()")
    class ActivateSeller {

        @Test
        @DisplayName("Happy path — changes status to ACTIVE from SUSPENDED")
        void activateSeller_fromSuspended_happyPath() {
            seller.setStatus(SellerStatus.SUSPENDED);

            service.activateSeller(sellerPublicId, actorId);

            assertThat(seller.getStatus()).isEqualTo(SellerStatus.ACTIVE);
            verify(transitionValidator).validate(SellerStatus.SUSPENDED, SellerStatus.ACTIVE);
            verify(sellerRepository).save(seller);

            ArgumentCaptor<SellerStatusHistory> captor = ArgumentCaptor.forClass(SellerStatusHistory.class);
            verify(historyRepository).save(captor.capture());
            assertThat(captor.getValue().getReason()).isEqualTo("Administrative reactivation");
        }

        @Test
        @DisplayName("Throws SellerNotFoundException when seller not found")
        void activateSeller_notFound_throws() {
            UUID unknownId = UUID.randomUUID();
            when(sellerRepository.findByPublicId(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.activateSeller(unknownId, actorId))
                    .isInstanceOf(SellerNotFoundException.class);
        }
    }

    // ── approveSeller() ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("approveSeller()")
    class ApproveSeller {

        @Test
        @DisplayName("Happy path — changes status to ACTIVE from UNDER_REVIEW")
        void approveSeller_happyPath() {
            seller.setStatus(SellerStatus.UNDER_REVIEW);

            service.approveSeller(sellerPublicId, actorId);

            assertThat(seller.getStatus()).isEqualTo(SellerStatus.ACTIVE);
            verify(transitionValidator).validate(SellerStatus.UNDER_REVIEW, SellerStatus.ACTIVE);
            verify(auditLogService).log(any());
        }
    }

    // ── blockSeller() ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("blockSeller()")
    class BlockSeller {

        @Test
        @DisplayName("Happy path — changes status to BLOCKED")
        void blockSeller_happyPath() {
            SellerStatusRequest request = new SellerStatusRequest();
            request.setReason("Fraudulent activity");

            service.blockSeller(sellerPublicId, actorId, request);

            assertThat(seller.getStatus()).isEqualTo(SellerStatus.BLOCKED);
            verify(transitionValidator).validate(SellerStatus.ACTIVE, SellerStatus.BLOCKED);

            ArgumentCaptor<SellerStatusHistory> captor = ArgumentCaptor.forClass(SellerStatusHistory.class);
            verify(historyRepository).save(captor.capture());
            assertThat(captor.getValue().getExpiresAt()).isNull();
        }

        @Test
        @DisplayName("Throws SellerStatusTransitionException when blocking from REJECTED")
        void blockSeller_fromRejected_throws() {
            seller.setStatus(SellerStatus.REJECTED);
            doThrow(new SellerStatusTransitionException(SellerStatus.REJECTED, SellerStatus.BLOCKED))
                    .when(transitionValidator).validate(SellerStatus.REJECTED, SellerStatus.BLOCKED);

            assertThatThrownBy(() -> service.blockSeller(sellerPublicId, actorId, new SellerStatusRequest()))
                    .isInstanceOf(SellerStatusTransitionException.class);
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private static void setPublicId(Seller s, UUID publicId) {
        try {
            java.lang.reflect.Field field = Seller.class.getDeclaredField("publicId");
            field.setAccessible(true);
            field.set(s, publicId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

package ecommerce.modules.store.service;

import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.store.dto.request.StoreStatusRequest;
import ecommerce.modules.store.dto.response.StoreResponse;
import ecommerce.modules.store.dto.response.StoreStatusHistoryResponse;
import ecommerce.modules.store.entity.Store;
import ecommerce.modules.store.entity.StoreStatusHistory;
import ecommerce.modules.store.enums.StoreStatus;
import ecommerce.modules.store.enums.StoreVisibility;
import ecommerce.modules.store.exception.StoreNotFoundException;
import ecommerce.modules.store.exception.StoreStatusTransitionException;
import ecommerce.modules.store.mapper.StoreMapper;
import ecommerce.modules.store.repository.StoreRepository;
import ecommerce.modules.store.repository.StoreStatusHistoryRepository;
import ecommerce.modules.store.service.impl.StoreStatusServiceImpl;
import ecommerce.modules.store.validation.StoreStatusTransitionValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoreStatusServiceImpl Tests")
class StoreStatusServiceImplTest {

    @Mock private StoreRepository               storeRepository;
    @Mock private StoreStatusHistoryRepository  historyRepository;
    @Mock private StoreStatusTransitionValidator transitionValidator;
    @Mock private StoreMapper                   mapper;
    @Mock private AuditLogService               auditLogService;

    @InjectMocks
    private StoreStatusServiceImpl service;

    private UUID  actorUserId;
    private UUID  storePublicId;
    private Store store;

    @BeforeEach
    void setUp() {
        actorUserId   = UUID.randomUUID();
        storePublicId = UUID.randomUUID();

        store = Store.builder()
                .id(1L)
                .publicId(storePublicId)
                .sellerId(10L)
                .storeName("Test Store")
                .slug("test-store")
                .status(StoreStatus.DRAFT)
                .visibility(StoreVisibility.PRIVATE)
                .isActive(true)
                .build();
    }

    // ── changeStatus() ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("changeStatus(storePublicId, actorUserId, request)")
    class ChangeStatus {

        @Test
        @DisplayName("DRAFT → PENDING_REVIEW: validates, updates store, saves history, logs audit")
        void changeStatus_draftToPendingReview_success() {
            store.setStatus(StoreStatus.DRAFT);
            StoreStatusRequest request = new StoreStatusRequest();
            request.setStatus(StoreStatus.PENDING_REVIEW);
            request.setReason("Ready for review");

            StoreResponse expected = StoreResponse.builder()
                    .id(storePublicId)
                    .status(StoreStatus.PENDING_REVIEW)
                    .build();

            when(storeRepository.findByPublicId(storePublicId)).thenReturn(Optional.of(store));
            when(storeRepository.save(store)).thenReturn(store);
            when(historyRepository.save(any(StoreStatusHistory.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toResponse(store)).thenReturn(expected);

            StoreResponse result = service.changeStatus(storePublicId, actorUserId, request);

            assertThat(result.getStatus()).isEqualTo(StoreStatus.PENDING_REVIEW);
            assertThat(store.getStatus()).isEqualTo(StoreStatus.PENDING_REVIEW);
            verify(transitionValidator).validate(StoreStatus.DRAFT, StoreStatus.PENDING_REVIEW);
            verify(storeRepository).save(store);
            verify(historyRepository).save(any(StoreStatusHistory.class));
            verify(auditLogService).log(any());
        }

        @Test
        @DisplayName("PENDING_REVIEW → ACTIVE: validates, updates store, saves history")
        void changeStatus_pendingReviewToActive_success() {
            store.setStatus(StoreStatus.PENDING_REVIEW);
            StoreStatusRequest request = new StoreStatusRequest();
            request.setStatus(StoreStatus.ACTIVE);

            when(storeRepository.findByPublicId(storePublicId)).thenReturn(Optional.of(store));
            when(storeRepository.save(store)).thenReturn(store);
            when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toResponse(store)).thenReturn(StoreResponse.builder().status(StoreStatus.ACTIVE).build());

            StoreResponse result = service.changeStatus(storePublicId, actorUserId, request);

            assertThat(store.getStatus()).isEqualTo(StoreStatus.ACTIVE);
            verify(transitionValidator).validate(StoreStatus.PENDING_REVIEW, StoreStatus.ACTIVE);
        }

        @Test
        @DisplayName("ACTIVE → PAUSED: validates, updates store, saves history")
        void changeStatus_activeToPaused_success() {
            store.setStatus(StoreStatus.ACTIVE);
            StoreStatusRequest request = new StoreStatusRequest();
            request.setStatus(StoreStatus.PAUSED);

            when(storeRepository.findByPublicId(storePublicId)).thenReturn(Optional.of(store));
            when(storeRepository.save(store)).thenReturn(store);
            when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toResponse(store)).thenReturn(StoreResponse.builder().status(StoreStatus.PAUSED).build());

            service.changeStatus(storePublicId, actorUserId, request);

            assertThat(store.getStatus()).isEqualTo(StoreStatus.PAUSED);
            verify(transitionValidator).validate(StoreStatus.ACTIVE, StoreStatus.PAUSED);
        }

        @Test
        @DisplayName("ACTIVE → SUSPENDED: validates, updates store, saves history")
        void changeStatus_activeToSuspended_success() {
            store.setStatus(StoreStatus.ACTIVE);
            StoreStatusRequest request = new StoreStatusRequest();
            request.setStatus(StoreStatus.SUSPENDED);
            request.setReason("Policy violation");

            when(storeRepository.findByPublicId(storePublicId)).thenReturn(Optional.of(store));
            when(storeRepository.save(store)).thenReturn(store);
            when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toResponse(store)).thenReturn(StoreResponse.builder().status(StoreStatus.SUSPENDED).build());

            service.changeStatus(storePublicId, actorUserId, request);

            assertThat(store.getStatus()).isEqualTo(StoreStatus.SUSPENDED);
            verify(transitionValidator).validate(StoreStatus.ACTIVE, StoreStatus.SUSPENDED);
        }

        @Test
        @DisplayName("ACTIVE → CLOSED: validates, updates store, saves history")
        void changeStatus_activeToClosed_success() {
            store.setStatus(StoreStatus.ACTIVE);
            StoreStatusRequest request = new StoreStatusRequest();
            request.setStatus(StoreStatus.CLOSED);

            when(storeRepository.findByPublicId(storePublicId)).thenReturn(Optional.of(store));
            when(storeRepository.save(store)).thenReturn(store);
            when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toResponse(store)).thenReturn(StoreResponse.builder().status(StoreStatus.CLOSED).build());

            service.changeStatus(storePublicId, actorUserId, request);

            assertThat(store.getStatus()).isEqualTo(StoreStatus.CLOSED);
            verify(transitionValidator).validate(StoreStatus.ACTIVE, StoreStatus.CLOSED);
        }

        @Test
        @DisplayName("Records correct previous and new status in the history entry")
        void changeStatus_savesCorrectHistoryEntry() {
            store.setStatus(StoreStatus.DRAFT);
            StoreStatusRequest request = new StoreStatusRequest();
            request.setStatus(StoreStatus.PENDING_REVIEW);
            request.setReason("Submitting for review");

            when(storeRepository.findByPublicId(storePublicId)).thenReturn(Optional.of(store));
            when(storeRepository.save(store)).thenReturn(store);
            when(mapper.toResponse(store)).thenReturn(StoreResponse.builder().build());

            ArgumentCaptor<StoreStatusHistory> historyCaptor = ArgumentCaptor.forClass(StoreStatusHistory.class);
            when(historyRepository.save(historyCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.changeStatus(storePublicId, actorUserId, request);

            StoreStatusHistory saved = historyCaptor.getValue();
            assertThat(saved.getPreviousStatus()).isEqualTo(StoreStatus.DRAFT);
            assertThat(saved.getNewStatus()).isEqualTo(StoreStatus.PENDING_REVIEW);
            assertThat(saved.getReason()).isEqualTo("Submitting for review");
            assertThat(saved.getChangedBy()).isEqualTo(actorUserId);
            assertThat(saved.getStoreId()).isEqualTo(store.getId());
        }

        @Test
        @DisplayName("Throws StoreStatusTransitionException for invalid transition (DRAFT → ACTIVE)")
        void changeStatus_invalidTransition_throwsTransitionException() {
            store.setStatus(StoreStatus.DRAFT);
            StoreStatusRequest request = new StoreStatusRequest();
            request.setStatus(StoreStatus.ACTIVE);

            when(storeRepository.findByPublicId(storePublicId)).thenReturn(Optional.of(store));
            doThrow(new StoreStatusTransitionException(StoreStatus.DRAFT, StoreStatus.ACTIVE))
                    .when(transitionValidator).validate(StoreStatus.DRAFT, StoreStatus.ACTIVE);

            assertThatThrownBy(() -> service.changeStatus(storePublicId, actorUserId, request))
                    .isInstanceOf(StoreStatusTransitionException.class)
                    .hasMessageContaining("DRAFT")
                    .hasMessageContaining("ACTIVE");

            verify(storeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws StoreNotFoundException when store does not exist")
        void changeStatus_storeNotFound_throwsStoreNotFoundException() {
            StoreStatusRequest request = new StoreStatusRequest();
            request.setStatus(StoreStatus.PENDING_REVIEW);

            when(storeRepository.findByPublicId(storePublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.changeStatus(storePublicId, actorUserId, request))
                    .isInstanceOf(StoreNotFoundException.class)
                    .hasMessageContaining(storePublicId.toString());
        }
    }

    // ── getHistory() ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getHistory(storePublicId)")
    class GetHistory {

        @Test
        @DisplayName("Returns mapped list of status history entries")
        void getHistory_returnsMappedList() {
            StoreStatusHistory entry = StoreStatusHistory.builder()
                    .storeId(store.getId())
                    .previousStatus(StoreStatus.DRAFT)
                    .newStatus(StoreStatus.PENDING_REVIEW)
                    .changedBy(actorUserId)
                    .build();
            StoreStatusHistoryResponse response = StoreStatusHistoryResponse.builder()
                    .previousStatus(StoreStatus.DRAFT)
                    .newStatus(StoreStatus.PENDING_REVIEW)
                    .build();

            when(storeRepository.findByPublicId(storePublicId)).thenReturn(Optional.of(store));
            when(historyRepository.findByStoreIdOrderByCreatedAtDesc(store.getId()))
                    .thenReturn(List.of(entry));
            when(mapper.toStatusHistoryResponse(entry)).thenReturn(response);

            List<StoreStatusHistoryResponse> result = service.getHistory(storePublicId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPreviousStatus()).isEqualTo(StoreStatus.DRAFT);
            assertThat(result.get(0).getNewStatus()).isEqualTo(StoreStatus.PENDING_REVIEW);
        }

        @Test
        @DisplayName("Returns empty list when no history exists")
        void getHistory_emptyHistory_returnsEmptyList() {
            when(storeRepository.findByPublicId(storePublicId)).thenReturn(Optional.of(store));
            when(historyRepository.findByStoreIdOrderByCreatedAtDesc(store.getId())).thenReturn(List.of());

            List<StoreStatusHistoryResponse> result = service.getHistory(storePublicId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Throws StoreNotFoundException when store does not exist")
        void getHistory_storeNotFound_throwsException() {
            when(storeRepository.findByPublicId(storePublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getHistory(storePublicId))
                    .isInstanceOf(StoreNotFoundException.class)
                    .hasMessageContaining(storePublicId.toString());
        }
    }
}

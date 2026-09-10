package ecommerce.modules.store.service;

import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.seller.entity.Seller;
import ecommerce.modules.store.dto.request.CreateStoreRequest;
import ecommerce.modules.store.dto.request.StoreVisibilityRequest;
import ecommerce.modules.store.dto.request.UpdateStoreRequest;
import ecommerce.modules.store.dto.response.StoreDetailResponse;
import ecommerce.modules.store.dto.response.StoreResponse;
import ecommerce.modules.store.dto.response.StoreStatusHistoryResponse;
import ecommerce.modules.store.dto.response.StoreSummaryResponse;
import ecommerce.modules.store.entity.Store;
import ecommerce.modules.store.entity.StoreSetting;
import ecommerce.modules.store.entity.StoreStatusHistory;
import ecommerce.modules.store.entity.StoreSummaryView;
import ecommerce.modules.store.enums.StoreStatus;
import ecommerce.modules.store.enums.StoreVisibility;
import ecommerce.modules.store.exception.StoreAlreadyExistsException;
import ecommerce.modules.store.exception.StoreNotFoundException;
import ecommerce.modules.store.exception.StoreStatusTransitionException;
import ecommerce.modules.store.mapper.StoreMapper;
import ecommerce.modules.store.policy.StoreOwnershipPolicy;
import ecommerce.modules.store.repository.StorePolicyRepository;
import ecommerce.modules.store.repository.StoreRepository;
import ecommerce.modules.store.repository.StoreSettingRepository;
import ecommerce.modules.store.repository.StoreStatusHistoryRepository;
import ecommerce.modules.store.repository.StoreSummaryViewRepository;
import ecommerce.modules.store.service.impl.StoreManagementServiceImpl;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoreManagementServiceImpl Tests")
class StoreManagementServiceImplTest {

    @Mock private StoreRepository               storeRepository;
    @Mock private StoreSettingRepository        settingRepository;
    @Mock private StorePolicyRepository         policyRepository;
    @Mock private StoreStatusHistoryRepository  statusHistoryRepository;
    @Mock private StoreSummaryViewRepository    summaryViewRepository;
    @Mock private StoreOwnershipPolicy          ownershipPolicy;
    @Mock private StoreSlugService              slugService;
    @Mock private StoreStatusTransitionValidator transitionValidator;
    @Mock private StoreMapper                   mapper;
    @Mock private AuditLogService               auditLogService;

    @InjectMocks
    private StoreManagementServiceImpl service;

    private UUID   userId;
    private UUID   storePublicId;
    private Seller seller;
    private Store  store;

    @BeforeEach
    void setUp() {
        userId        = UUID.randomUUID();
        storePublicId = UUID.randomUUID();

        seller = Seller.builder()
                .id(10L)
                .publicId(UUID.randomUUID())
                .sellerNumber("SEL-000001")
                .ownerUserId(userId)
                .build();

        store = Store.builder()
                .id(1L)
                .publicId(storePublicId)
                .sellerId(seller.getId())
                .storeName("My Test Store")
                .slug("my-test-store")
                .status(StoreStatus.DRAFT)
                .visibility(StoreVisibility.PRIVATE)
                .isActive(true)
                .build();
    }

    // ── createStore() ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createStore(actorUserId, request)")
    class CreateStore {

        @Test
        @DisplayName("Happy path — creates store, persists settings, logs audit, returns StoreDetailResponse")
        void createStore_happyPath_storeCreatedWithSettingsAndAudit() {
            CreateStoreRequest request = new CreateStoreRequest();
            request.setStoreName("My Test Store");
            request.setBusinessEmail("store@test.com");

            StoreDetailResponse expectedResponse = StoreDetailResponse.builder()
                    .id(storePublicId)
                    .storeName("My Test Store")
                    .status(StoreStatus.DRAFT)
                    .build();

            when(ownershipPolicy.resolveSeller(userId)).thenReturn(seller);
            when(storeRepository.existsBySellerIdAndStatusNot(seller.getId(), StoreStatus.ARCHIVED)).thenReturn(false);
            when(slugService.generateSlug("My Test Store")).thenReturn("my-test-store");
            when(storeRepository.save(any(Store.class))).thenReturn(store);
            when(settingRepository.save(any(StoreSetting.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toDetailResponse(any(Store.class), any(StoreSetting.class), any())).thenReturn(expectedResponse);

            StoreDetailResponse result = service.createStore(userId, request);

            assertThat(result).isNotNull();
            assertThat(result.getStoreName()).isEqualTo("My Test Store");
            assertThat(result.getStatus()).isEqualTo(StoreStatus.DRAFT);
            verify(storeRepository).save(any(Store.class));
            verify(settingRepository).save(any(StoreSetting.class));
            verify(auditLogService).log(any());
        }

        @Test
        @DisplayName("Uses provided slug directly when request includes a non-blank slug")
        void createStore_withExplicitSlug_usesProvidedSlug() {
            CreateStoreRequest request = new CreateStoreRequest();
            request.setStoreName("My Test Store");
            request.setSlug("custom-slug");

            when(ownershipPolicy.resolveSeller(userId)).thenReturn(seller);
            when(storeRepository.existsBySellerIdAndStatusNot(seller.getId(), StoreStatus.ARCHIVED)).thenReturn(false);
            when(storeRepository.save(any(Store.class))).thenReturn(store);
            when(settingRepository.save(any(StoreSetting.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toDetailResponse(any(), any(), any())).thenReturn(StoreDetailResponse.builder().build());

            service.createStore(userId, request);

            verify(slugService, never()).generateSlug(any());
            verify(slugService).validateAndReserve(eq("custom-slug"), any());
        }

        @Test
        @DisplayName("Generates slug from storeName when request slug is blank")
        void createStore_noSlugInRequest_generatesFromName() {
            CreateStoreRequest request = new CreateStoreRequest();
            request.setStoreName("My Test Store");
            // slug is null

            when(ownershipPolicy.resolveSeller(userId)).thenReturn(seller);
            when(storeRepository.existsBySellerIdAndStatusNot(seller.getId(), StoreStatus.ARCHIVED)).thenReturn(false);
            when(slugService.generateSlug("My Test Store")).thenReturn("my-test-store");
            when(storeRepository.save(any(Store.class))).thenReturn(store);
            when(settingRepository.save(any(StoreSetting.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toDetailResponse(any(), any(), any())).thenReturn(StoreDetailResponse.builder().build());

            service.createStore(userId, request);

            verify(slugService).generateSlug("My Test Store");
        }

        @Test
        @DisplayName("Sets DRAFT status and PRIVATE visibility on new store")
        void createStore_setsCorrectInitialState() {
            CreateStoreRequest request = new CreateStoreRequest();
            request.setStoreName("New Store");

            when(ownershipPolicy.resolveSeller(userId)).thenReturn(seller);
            when(storeRepository.existsBySellerIdAndStatusNot(seller.getId(), StoreStatus.ARCHIVED)).thenReturn(false);
            when(slugService.generateSlug("New Store")).thenReturn("new-store");

            ArgumentCaptor<Store> storeCaptor = ArgumentCaptor.forClass(Store.class);
            when(storeRepository.save(storeCaptor.capture())).thenReturn(store);
            when(settingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toDetailResponse(any(), any(), any())).thenReturn(StoreDetailResponse.builder().build());

            service.createStore(userId, request);

            Store saved = storeCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(StoreStatus.DRAFT);
            assertThat(saved.getVisibility()).isEqualTo(StoreVisibility.PRIVATE);
            assertThat(saved.getIsActive()).isTrue();
            assertThat(saved.getSellerId()).isEqualTo(seller.getId());
        }

        @Test
        @DisplayName("Throws StoreAlreadyExistsException when seller already has an active (non-ARCHIVED) store")
        void createStore_duplicateStore_throwsStoreAlreadyExistsException() {
            CreateStoreRequest request = new CreateStoreRequest();
            request.setStoreName("Another Store");

            when(ownershipPolicy.resolveSeller(userId)).thenReturn(seller);
            when(storeRepository.existsBySellerIdAndStatusNot(seller.getId(), StoreStatus.ARCHIVED)).thenReturn(true);

            assertThatThrownBy(() -> service.createStore(userId, request))
                    .isInstanceOf(StoreAlreadyExistsException.class);

            verify(storeRepository, never()).save(any());
        }
    }

    // ── getMyStore() ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMyStore(actorUserId)")
    class GetMyStore {

        @Test
        @DisplayName("Returns StoreDetailResponse for the authenticated seller's store")
        void getMyStore_returnsDetailResponse() {
            StoreSetting setting = StoreSetting.builder().store(store).build();
            StoreDetailResponse expected = StoreDetailResponse.builder()
                    .id(storePublicId)
                    .storeName("My Test Store")
                    .status(StoreStatus.DRAFT)
                    .build();

            when(ownershipPolicy.resolveOwnStore(userId)).thenReturn(store);
            when(settingRepository.findByStore_Id(store.getId())).thenReturn(Optional.of(setting));
            when(policyRepository.findByStoreIdAndIsActiveTrue(store.getId())).thenReturn(List.of());
            when(mapper.toDetailResponse(store, setting, List.of())).thenReturn(expected);

            StoreDetailResponse result = service.getMyStore(userId);

            assertThat(result).isNotNull();
            assertThat(result.getStoreName()).isEqualTo("My Test Store");
            verify(ownershipPolicy).resolveOwnStore(userId);
        }

        @Test
        @DisplayName("Throws StoreNotFoundException when seller has no active store")
        void getMyStore_noStore_throwsStoreNotFoundException() {
            when(ownershipPolicy.resolveOwnStore(userId))
                    .thenThrow(new StoreNotFoundException("No active store found for current seller"));

            assertThatThrownBy(() -> service.getMyStore(userId))
                    .isInstanceOf(StoreNotFoundException.class)
                    .hasMessageContaining("No active store found");
        }
    }

    // ── getStoreByPublicId() ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getStoreByPublicId(publicId)")
    class GetStoreByPublicId {

        @Test
        @DisplayName("Returns StoreDetailResponse when store exists for given publicId")
        void getStoreByPublicId_found_returnsDetailResponse() {
            StoreSetting setting = StoreSetting.builder().store(store).build();
            StoreDetailResponse expected = StoreDetailResponse.builder()
                    .id(storePublicId)
                    .storeName("My Test Store")
                    .build();

            when(storeRepository.findByPublicId(storePublicId)).thenReturn(Optional.of(store));
            when(settingRepository.findByStore_Id(store.getId())).thenReturn(Optional.of(setting));
            when(policyRepository.findByStoreIdAndIsActiveTrue(store.getId())).thenReturn(List.of());
            when(mapper.toDetailResponse(store, setting, List.of())).thenReturn(expected);

            StoreDetailResponse result = service.getStoreByPublicId(storePublicId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(storePublicId);
        }

        @Test
        @DisplayName("Throws StoreNotFoundException when no store matches publicId")
        void getStoreByPublicId_notFound_throwsStoreNotFoundException() {
            when(storeRepository.findByPublicId(storePublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getStoreByPublicId(storePublicId))
                    .isInstanceOf(StoreNotFoundException.class)
                    .hasMessageContaining(storePublicId.toString());
        }
    }

    // ── getStoreBySlug() ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("getStoreBySlug(slug)")
    class GetStoreBySlug {

        @Test
        @DisplayName("Returns StoreSummaryResponse when store exists for given slug")
        void getStoreBySlug_found_returnsSummaryResponse() {
            StoreSummaryView view = mock(StoreSummaryView.class);
            StoreSummaryResponse expected = StoreSummaryResponse.builder()
                    .id(storePublicId)
                    .slug("my-test-store")
                    .storeName("My Test Store")
                    .build();

            when(summaryViewRepository.findBySlug("my-test-store")).thenReturn(Optional.of(view));
            when(mapper.toSummary(view)).thenReturn(expected);

            StoreSummaryResponse result = service.getStoreBySlug("my-test-store");

            assertThat(result).isNotNull();
            assertThat(result.getSlug()).isEqualTo("my-test-store");
        }

        @Test
        @DisplayName("Throws StoreNotFoundException when no store matches the slug")
        void getStoreBySlug_notFound_throwsStoreNotFoundException() {
            when(summaryViewRepository.findBySlug("unknown-slug")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getStoreBySlug("unknown-slug"))
                    .isInstanceOf(StoreNotFoundException.class)
                    .hasMessageContaining("unknown-slug");
        }
    }

    // ── updateStore() ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateStore(actorUserId, request)")
    class UpdateStore {

        @Test
        @DisplayName("Applies partial updates to store and logs audit")
        void updateStore_appliesChangesAndLogsAudit() {
            UpdateStoreRequest request = new UpdateStoreRequest();
            request.setStoreName("Updated Name");
            request.setBusinessEmail("updated@test.com");
            request.setWebsite("https://updated.com");

            StoreResponse expected = StoreResponse.builder()
                    .id(storePublicId)
                    .storeName("Updated Name")
                    .build();

            when(ownershipPolicy.resolveOwnStore(userId)).thenReturn(store);
            when(storeRepository.save(store)).thenReturn(store);
            when(mapper.toResponse(store)).thenReturn(expected);

            StoreResponse result = service.updateStore(userId, request);

            assertThat(result).isNotNull();
            assertThat(result.getStoreName()).isEqualTo("Updated Name");
            assertThat(store.getStoreName()).isEqualTo("Updated Name");
            assertThat(store.getBusinessEmail()).isEqualTo("updated@test.com");
            assertThat(store.getWebsite()).isEqualTo("https://updated.com");
            verify(storeRepository).save(store);
            verify(auditLogService).log(any());
        }

        @Test
        @DisplayName("Skips null fields — only non-null fields are applied")
        void updateStore_skipsNullFields_preservesExistingValues() {
            store.setStoreName("Original Name");
            store.setDescription("Original Description");

            UpdateStoreRequest request = new UpdateStoreRequest();
            request.setStoreName("New Name");
            // description is null — should not overwrite

            when(ownershipPolicy.resolveOwnStore(userId)).thenReturn(store);
            when(storeRepository.save(store)).thenReturn(store);
            when(mapper.toResponse(store)).thenReturn(StoreResponse.builder().build());

            service.updateStore(userId, request);

            assertThat(store.getStoreName()).isEqualTo("New Name");
            assertThat(store.getDescription()).isEqualTo("Original Description");
        }
    }

    // ── updateVisibility() ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateVisibility(actorUserId, request)")
    class UpdateVisibility {

        @Test
        @DisplayName("Sets new visibility on store, saves, and logs audit")
        void updateVisibility_setsVisibilityAndLogs() {
            StoreVisibilityRequest request = new StoreVisibilityRequest();
            request.setVisibility(StoreVisibility.PUBLIC);

            StoreResponse expected = StoreResponse.builder()
                    .id(storePublicId)
                    .visibility(StoreVisibility.PUBLIC)
                    .build();

            when(ownershipPolicy.resolveOwnStore(userId)).thenReturn(store);
            when(storeRepository.save(store)).thenReturn(store);
            when(mapper.toResponse(store)).thenReturn(expected);

            StoreResponse result = service.updateVisibility(userId, request);

            assertThat(result.getVisibility()).isEqualTo(StoreVisibility.PUBLIC);
            assertThat(store.getVisibility()).isEqualTo(StoreVisibility.PUBLIC);
            verify(storeRepository).save(store);
            verify(auditLogService).log(any());
        }
    }

    // ── submitForReview() ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("submitForReview(actorUserId)")
    class SubmitForReview {

        @Test
        @DisplayName("Transitions DRAFT → PENDING_REVIEW, records history, and logs audit")
        void submitForReview_draftToPendingReview_success() {
            store.setStatus(StoreStatus.DRAFT);
            StoreResponse expected = StoreResponse.builder()
                    .id(storePublicId)
                    .status(StoreStatus.PENDING_REVIEW)
                    .build();

            when(ownershipPolicy.resolveOwnStore(userId)).thenReturn(store);
            when(storeRepository.save(store)).thenReturn(store);
            when(statusHistoryRepository.save(any(StoreStatusHistory.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toResponse(store)).thenReturn(expected);

            StoreResponse result = service.submitForReview(userId);

            assertThat(result.getStatus()).isEqualTo(StoreStatus.PENDING_REVIEW);
            assertThat(store.getStatus()).isEqualTo(StoreStatus.PENDING_REVIEW);
            verify(transitionValidator).validate(StoreStatus.DRAFT, StoreStatus.PENDING_REVIEW);
            verify(statusHistoryRepository).save(any(StoreStatusHistory.class));
            verify(auditLogService).log(any());
        }

        @Test
        @DisplayName("Records correct previous and new status in history entry")
        void submitForReview_recordsCorrectStatusHistory() {
            store.setStatus(StoreStatus.DRAFT);

            when(ownershipPolicy.resolveOwnStore(userId)).thenReturn(store);
            when(storeRepository.save(store)).thenReturn(store);
            when(mapper.toResponse(store)).thenReturn(StoreResponse.builder().build());

            ArgumentCaptor<StoreStatusHistory> historyCaptor = ArgumentCaptor.forClass(StoreStatusHistory.class);
            when(statusHistoryRepository.save(historyCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.submitForReview(userId);

            StoreStatusHistory saved = historyCaptor.getValue();
            assertThat(saved.getPreviousStatus()).isEqualTo(StoreStatus.DRAFT);
            assertThat(saved.getNewStatus()).isEqualTo(StoreStatus.PENDING_REVIEW);
            assertThat(saved.getChangedBy()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Throws StoreStatusTransitionException for invalid transition (e.g. ACTIVE → PENDING_REVIEW)")
        void submitForReview_invalidTransition_throwsTransitionException() {
            store.setStatus(StoreStatus.ACTIVE);

            when(ownershipPolicy.resolveOwnStore(userId)).thenReturn(store);
            org.mockito.Mockito.doThrow(new StoreStatusTransitionException(StoreStatus.ACTIVE, StoreStatus.PENDING_REVIEW))
                    .when(transitionValidator).validate(StoreStatus.ACTIVE, StoreStatus.PENDING_REVIEW);

            assertThatThrownBy(() -> service.submitForReview(userId))
                    .isInstanceOf(StoreStatusTransitionException.class)
                    .hasMessageContaining("ACTIVE");

            verify(storeRepository, never()).save(any());
        }
    }

    // ── searchStores() ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("searchStores(params, pageable)")
    class SearchStores {

        @Test
        @DisplayName("Delegates to summaryViewRepository with spec and returns mapped page")
        void searchStores_delegatesToRepository() {
            StoreSummaryView view = mock(StoreSummaryView.class);
            StoreSummaryResponse summary = StoreSummaryResponse.builder()
                    .id(storePublicId)
                    .storeName("My Test Store")
                    .build();
            Page<StoreSummaryView> viewPage = new PageImpl<>(List.of(view));
            Pageable pageable = Pageable.ofSize(20);

            ecommerce.modules.store.dto.request.StoreSearchRequest params =
                    new ecommerce.modules.store.dto.request.StoreSearchRequest();

            when(summaryViewRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(viewPage);
            when(mapper.toSummary(view)).thenReturn(summary);

            Page<StoreSummaryResponse> result = service.searchStores(params, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStoreName()).isEqualTo("My Test Store");
        }
    }

    // ── getStatusHistory() ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("getStatusHistory(storePublicId)")
    class GetStatusHistory {

        @Test
        @DisplayName("Returns mapped history entries for an existing store")
        void getStatusHistory_returnsMappedList() {
            StoreStatusHistory historyEntry = StoreStatusHistory.builder()
                    .storeId(store.getId())
                    .previousStatus(StoreStatus.DRAFT)
                    .newStatus(StoreStatus.PENDING_REVIEW)
                    .changedBy(userId)
                    .build();
            StoreStatusHistoryResponse historyResponse = StoreStatusHistoryResponse.builder()
                    .previousStatus(StoreStatus.DRAFT)
                    .newStatus(StoreStatus.PENDING_REVIEW)
                    .build();

            when(storeRepository.findByPublicId(storePublicId)).thenReturn(Optional.of(store));
            when(statusHistoryRepository.findByStoreIdOrderByCreatedAtDesc(store.getId()))
                    .thenReturn(List.of(historyEntry));
            when(mapper.toStatusHistoryResponse(historyEntry)).thenReturn(historyResponse);

            List<StoreStatusHistoryResponse> result = service.getStatusHistory(storePublicId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPreviousStatus()).isEqualTo(StoreStatus.DRAFT);
            assertThat(result.get(0).getNewStatus()).isEqualTo(StoreStatus.PENDING_REVIEW);
        }

        @Test
        @DisplayName("Throws StoreNotFoundException when store does not exist")
        void getStatusHistory_storeNotFound_throwsException() {
            when(storeRepository.findByPublicId(storePublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getStatusHistory(storePublicId))
                    .isInstanceOf(StoreNotFoundException.class)
                    .hasMessageContaining(storePublicId.toString());
        }
    }
}

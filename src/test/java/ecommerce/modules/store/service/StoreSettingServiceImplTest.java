package ecommerce.modules.store.service;

import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.store.dto.request.StoreSettingRequest;
import ecommerce.modules.store.dto.response.StoreSettingResponse;
import ecommerce.modules.store.entity.Store;
import ecommerce.modules.store.entity.StoreSetting;
import ecommerce.modules.store.enums.StoreStatus;
import ecommerce.modules.store.enums.StoreVisibility;
import ecommerce.modules.store.mapper.StoreMapper;
import ecommerce.modules.store.policy.StoreOwnershipPolicy;
import ecommerce.modules.store.repository.StoreSettingRepository;
import ecommerce.modules.store.service.impl.StoreSettingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoreSettingServiceImpl Tests")
class StoreSettingServiceImplTest {

    @Mock private StoreOwnershipPolicy   ownershipPolicy;
    @Mock private StoreSettingRepository settingRepository;
    @Mock private StoreMapper            mapper;
    @Mock private AuditLogService        auditLogService;

    @InjectMocks
    private StoreSettingServiceImpl service;

    private UUID         userId;
    private Store        store;
    private StoreSetting existingSetting;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        store = Store.builder()
                .id(1L)
                .publicId(UUID.randomUUID())
                .sellerId(10L)
                .storeName("Test Store")
                .slug("test-store")
                .status(StoreStatus.ACTIVE)
                .visibility(StoreVisibility.PUBLIC)
                .isActive(true)
                .build();

        existingSetting = StoreSetting.builder()
                .id(1L)
                .store(store)
                .currency("GHS")
                .timezone("Africa/Accra")
                .language("en")
                .orderNotifications(true)
                .customerNotifications(true)
                .build();
    }

    // ── getSettings() ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getSettings(actorUserId)")
    class GetSettings {

        @Test
        @DisplayName("Returns setting response when setting exists for the seller's store")
        void getSettings_existingSetting_returnsMappedResponse() {
            StoreSettingResponse expected = StoreSettingResponse.builder()
                    .currency("GHS")
                    .timezone("Africa/Accra")
                    .language("en")
                    .orderNotifications(true)
                    .customerNotifications(true)
                    .build();

            when(ownershipPolicy.resolveOwnStore(userId)).thenReturn(store);
            when(settingRepository.findByStore_Id(store.getId())).thenReturn(Optional.of(existingSetting));
            when(mapper.toSettingResponse(existingSetting)).thenReturn(expected);

            StoreSettingResponse result = service.getSettings(userId);

            assertThat(result).isNotNull();
            assertThat(result.getCurrency()).isEqualTo("GHS");
            assertThat(result.getTimezone()).isEqualTo("Africa/Accra");
            assertThat(result.getLanguage()).isEqualTo("en");
            verify(ownershipPolicy).resolveOwnStore(userId);
            verify(settingRepository).findByStore_Id(store.getId());
        }

        @Test
        @DisplayName("Falls back to new default setting when no setting exists in database")
        void getSettings_noSettingInDb_returnsDefaultSetting() {
            StoreSettingResponse expected = StoreSettingResponse.builder()
                    .currency("GHS")
                    .timezone("Africa/Accra")
                    .language("en")
                    .build();

            when(ownershipPolicy.resolveOwnStore(userId)).thenReturn(store);
            when(settingRepository.findByStore_Id(store.getId())).thenReturn(Optional.empty());
            when(mapper.toSettingResponse(any(StoreSetting.class))).thenReturn(expected);

            StoreSettingResponse result = service.getSettings(userId);

            assertThat(result).isNotNull();
            verify(settingRepository).findByStore_Id(store.getId());
        }

        @Test
        @DisplayName("Fallback setting is linked to the current store")
        void getSettings_noSettingInDb_fallbackLinkedToStore() {
            when(ownershipPolicy.resolveOwnStore(userId)).thenReturn(store);
            when(settingRepository.findByStore_Id(store.getId())).thenReturn(Optional.empty());

            ArgumentCaptor<StoreSetting> settingCaptor = ArgumentCaptor.forClass(StoreSetting.class);
            when(mapper.toSettingResponse(settingCaptor.capture())).thenReturn(StoreSettingResponse.builder().build());

            service.getSettings(userId);

            StoreSetting fallback = settingCaptor.getValue();
            assertThat(fallback.getStore()).isEqualTo(store);
        }
    }

    // ── updateSettings() ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateSettings(actorUserId, request)")
    class UpdateSettings {

        @Test
        @DisplayName("Applies all non-null fields from request and persists the setting")
        void updateSettings_allFieldsProvided_appliesAllChanges() {
            StoreSettingRequest request = new StoreSettingRequest();
            request.setCurrency("USD");
            request.setTimezone("UTC");
            request.setLanguage("fr");
            request.setOrderNotifications(false);
            request.setCustomerNotifications(false);

            StoreSettingResponse expected = StoreSettingResponse.builder()
                    .currency("USD")
                    .timezone("UTC")
                    .language("fr")
                    .orderNotifications(false)
                    .customerNotifications(false)
                    .build();

            when(ownershipPolicy.resolveOwnStore(userId)).thenReturn(store);
            when(settingRepository.findByStore_Id(store.getId())).thenReturn(Optional.of(existingSetting));
            when(settingRepository.save(existingSetting)).thenReturn(existingSetting);
            when(mapper.toSettingResponse(existingSetting)).thenReturn(expected);

            StoreSettingResponse result = service.updateSettings(userId, request);

            assertThat(result.getCurrency()).isEqualTo("USD");
            assertThat(result.getTimezone()).isEqualTo("UTC");
            assertThat(result.getLanguage()).isEqualTo("fr");
            assertThat(existingSetting.getCurrency()).isEqualTo("USD");
            assertThat(existingSetting.getTimezone()).isEqualTo("UTC");
            assertThat(existingSetting.getLanguage()).isEqualTo("fr");
            assertThat(existingSetting.getOrderNotifications()).isFalse();
            assertThat(existingSetting.getCustomerNotifications()).isFalse();
            verify(settingRepository).save(existingSetting);
        }

        @Test
        @DisplayName("Skips null fields — only non-null values are applied")
        void updateSettings_partialUpdate_skipsNullFields() {
            existingSetting.setCurrency("GHS");
            existingSetting.setTimezone("Africa/Accra");

            StoreSettingRequest request = new StoreSettingRequest();
            request.setCurrency("EUR"); // only currency changed
            // all other fields null

            when(ownershipPolicy.resolveOwnStore(userId)).thenReturn(store);
            when(settingRepository.findByStore_Id(store.getId())).thenReturn(Optional.of(existingSetting));
            when(settingRepository.save(existingSetting)).thenReturn(existingSetting);
            when(mapper.toSettingResponse(existingSetting)).thenReturn(StoreSettingResponse.builder().build());

            service.updateSettings(userId, request);

            assertThat(existingSetting.getCurrency()).isEqualTo("EUR");
            assertThat(existingSetting.getTimezone()).isEqualTo("Africa/Accra"); // unchanged
        }

        @Test
        @DisplayName("Logs audit after successful settings update")
        void updateSettings_logsAuditAction() {
            StoreSettingRequest request = new StoreSettingRequest();
            request.setCurrency("USD");

            when(ownershipPolicy.resolveOwnStore(userId)).thenReturn(store);
            when(settingRepository.findByStore_Id(store.getId())).thenReturn(Optional.of(existingSetting));
            when(settingRepository.save(any())).thenReturn(existingSetting);
            when(mapper.toSettingResponse(any())).thenReturn(StoreSettingResponse.builder().build());

            service.updateSettings(userId, request);

            verify(auditLogService).log(any());
        }

        @Test
        @DisplayName("Creates a new setting when none exists in database before updating")
        void updateSettings_noExistingSetting_createsAndUpdates() {
            StoreSettingRequest request = new StoreSettingRequest();
            request.setCurrency("USD");

            when(ownershipPolicy.resolveOwnStore(userId)).thenReturn(store);
            when(settingRepository.findByStore_Id(store.getId())).thenReturn(Optional.empty());

            ArgumentCaptor<StoreSetting> settingCaptor = ArgumentCaptor.forClass(StoreSetting.class);
            when(settingRepository.save(settingCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toSettingResponse(any())).thenReturn(StoreSettingResponse.builder().build());

            service.updateSettings(userId, request);

            StoreSetting saved = settingCaptor.getValue();
            assertThat(saved.getStore()).isEqualTo(store);
            assertThat(saved.getCurrency()).isEqualTo("USD");
        }
    }
}

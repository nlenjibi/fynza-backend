package ecommerce.modules.store.service;

import ecommerce.common.exception.ForbiddenException;
import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.store.dto.request.StorePolicyRequest;
import ecommerce.modules.store.dto.response.StorePolicyResponse;
import ecommerce.modules.store.entity.Store;
import ecommerce.modules.store.entity.StorePolicy;
import ecommerce.modules.store.enums.StorePolicyStatus;
import ecommerce.modules.store.enums.StorePolicyType;
import ecommerce.modules.store.enums.StoreStatus;
import ecommerce.modules.store.enums.StoreVisibility;
import ecommerce.modules.store.exception.StorePolicyNotFoundException;
import ecommerce.modules.store.mapper.StoreMapper;
import ecommerce.modules.store.policy.StoreOwnershipPolicy;
import ecommerce.modules.store.repository.StorePolicyRepository;
import ecommerce.modules.store.service.impl.StorePolicyServiceImpl;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StorePolicyServiceImpl Tests")
class StorePolicyServiceImplTest {

    @Mock private StoreOwnershipPolicy  ownershipPolicy;
    @Mock private StorePolicyRepository policyRepository;
    @Mock private StoreMapper           mapper;
    @Mock private AuditLogService       auditLogService;

    @InjectMocks
    private StorePolicyServiceImpl service;

    private UUID         userId;
    private Store        store;
    private StorePolicy  policy;
    private UUID         policyPublicId;

    @BeforeEach
    void setUp() {
        userId         = UUID.randomUUID();
        policyPublicId = UUID.randomUUID();

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

        policy = StorePolicy.builder()
                .id(5L)
                .publicId(policyPublicId)
                .storeId(store.getId())
                .type(StorePolicyType.RETURN)
                .title("Return Policy")
                .content("You can return within 30 days.")
                .version(1)
                .status(StorePolicyStatus.DRAFT)
                .isActive(true)
                .build();
    }

    // -- getPolicies() --

    @Nested
    @DisplayName("getPolicies(actorUserId)")
    class GetPolicies {

        @Test
        @DisplayName("Returns list of active policies for the seller's store")
        void getPolicies_returnsActivePolicies() {
            StorePolicyResponse policyResponse = StorePolicyResponse.builder()
                    .id(policyPublicId)
                    .type(StorePolicyType.RETURN)
                    .title("Return Policy")
                    .version(1)
                    .status(StorePolicyStatus.DRAFT)
                    .build();

            when(ownershipPolicy.resolveOwnStore(userId)).thenReturn(store);
            when(policyRepository.findByStoreIdAndIsActiveTrue(store.getId())).thenReturn(List.of(policy));
            when(mapper.toPolicyResponse(policy)).thenReturn(policyResponse);

            List<StorePolicyResponse> result = service.getPolicies(userId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Return Policy");
            verify(policyRepository).findByStoreIdAndIsActiveTrue(store.getId());
        }

        @Test
        @DisplayName("Returns empty list when no active policies exist")
        void getPolicies_noPolicies_returnsEmptyList() {
            when(ownershipPolicy.resolveOwnStore(userId)).thenReturn(store);
            when(policyRepository.findByStoreIdAndIsActiveTrue(store.getId())).thenReturn(List.of());

            List<StorePolicyResponse> result = service.getPolicies(userId);

            assertThat(result).isEmpty();
        }
    }

    // -- createPolicy() --

    @Nested
    @DisplayName("createPolicy(actorUserId, request)")
    class CreatePolicy {

        @Test
        @DisplayName("Saves policy with correct fields and logs audit")
        void createPolicy_savesWithCorrectFieldsAndLogsAudit() {
            StorePolicyRequest request = new StorePolicyRequest();
            request.setType(StorePolicyType.RETURN);
            request.setTitle("Return Policy");
            request.setContent("30-day return.");
            request.setStatus(StorePolicyStatus.ACTIVE);

            StorePolicyResponse expected = StorePolicyResponse.builder()
                    .id(policyPublicId)
                    .type(StorePolicyType.RETURN)
                    .title("Return Policy")
                    .status(StorePolicyStatus.ACTIVE)
                    .version(1)
                    .build();

            when(ownershipPolicy.resolveOwnStore(userId)).thenReturn(store);

            ArgumentCaptor<StorePolicy> policyCaptor = ArgumentCaptor.forClass(StorePolicy.class);
            when(policyRepository.save(policyCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toPolicyResponse(any(StorePolicy.class))).thenReturn(expected);

            StorePolicyResponse result = service.createPolicy(userId, request);

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("Return Policy");

            StorePolicy saved = policyCaptor.getValue();
            assertThat(saved.getStoreId()).isEqualTo(store.getId());
            assertThat(saved.getType()).isEqualTo(StorePolicyType.RETURN);
            assertThat(saved.getTitle()).isEqualTo("Return Policy");
            assertThat(saved.getContent()).isEqualTo("30-day return.");
            assertThat(saved.getStatus()).isEqualTo(StorePolicyStatus.ACTIVE);
            verify(auditLogService).log(any());
        }

        @Test
        @DisplayName("Defaults to DRAFT status when request status is null")
        void createPolicy_nullStatus_defaultsToDraft() {
            StorePolicyRequest request = new StorePolicyRequest();
            request.setType(StorePolicyType.SHIPPING);
            request.setTitle("Shipping Policy");
            request.setContent("Free shipping over 100 GHS.");

            when(ownershipPolicy.resolveOwnStore(userId)).thenReturn(store);

            ArgumentCaptor<StorePolicy> policyCaptor = ArgumentCaptor.forClass(StorePolicy.class);
            when(policyRepository.save(policyCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toPolicyResponse(any(StorePolicy.class))).thenReturn(StorePolicyResponse.builder().build());

            service.createPolicy(userId, request);

            assertThat(policyCaptor.getValue().getStatus()).isEqualTo(StorePolicyStatus.DRAFT);
        }
    }

    // -- updatePolicy() --

    @Nested
    @DisplayName("updatePolicy(actorUserId, policyPublicId, request)")
    class UpdatePolicy {

        @Test
        @DisplayName("Updates title, content, status and increments version")
        void updatePolicy_updatesFieldsAndIncrementsVersion() {
            StorePolicyRequest request = new StorePolicyRequest();
            request.setTitle("Updated Return Policy");
            request.setContent("Updated content.");
            request.setStatus(StorePolicyStatus.ACTIVE);

            StorePolicyResponse expected = StorePolicyResponse.builder()
                    .id(policyPublicId)
                    .title("Updated Return Policy")
                    .version(2)
                    .build();

            when(ownershipPolicy.resolveOwnStore(userId)).thenReturn(store);
            when(policyRepository.findByPublicId(policyPublicId)).thenReturn(Optional.of(policy));
            when(policyRepository.save(policy)).thenReturn(policy);
            when(mapper.toPolicyResponse(policy)).thenReturn(expected);

            service.updatePolicy(userId, policyPublicId, request);

            assertThat(policy.getTitle()).isEqualTo("Updated Return Policy");
            assertThat(policy.getContent()).isEqualTo("Updated content.");
            assertThat(policy.getStatus()).isEqualTo(StorePolicyStatus.ACTIVE);
            assertThat(policy.getVersion()).isEqualTo(2);
            verify(policyRepository).save(policy);
            verify(auditLogService).log(any());
        }

        @Test
        @DisplayName("Throws ForbiddenException when policy belongs to a different store")
        void updatePolicy_wrongStore_throwsForbiddenException() {
            Store differentStore = Store.builder()
                    .id(999L)
                    .publicId(UUID.randomUUID())
                    .build();

            when(ownershipPolicy.resolveOwnStore(userId)).thenReturn(differentStore);
            when(policyRepository.findByPublicId(policyPublicId)).thenReturn(Optional.of(policy));

            assertThatThrownBy(() -> service.updatePolicy(userId, policyPublicId, new StorePolicyRequest()))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("does not belong");

            verify(policyRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws StorePolicyNotFoundException when policy does not exist")
        void updatePolicy_policyNotFound_throwsStorePolicyNotFoundException() {
            when(ownershipPolicy.resolveOwnStore(userId)).thenReturn(store);
            when(policyRepository.findByPublicId(policyPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updatePolicy(userId, policyPublicId, new StorePolicyRequest()))
                    .isInstanceOf(StorePolicyNotFoundException.class)
                    .hasMessageContaining(policyPublicId.toString());
        }
    }

    // -- deletePolicy() --

    @Nested
    @DisplayName("deletePolicy(actorUserId, policyPublicId)")
    class DeletePolicy {

        @Test
        @DisplayName("Soft-deletes policy by setting isActive=false and logs audit")
        void deletePolicy_setsIsActiveFalseAndLogsAudit() {
            when(ownershipPolicy.resolveOwnStore(userId)).thenReturn(store);
            when(policyRepository.findByPublicId(policyPublicId)).thenReturn(Optional.of(policy));
            when(policyRepository.save(policy)).thenReturn(policy);

            service.deletePolicy(userId, policyPublicId);

            assertThat(policy.getIsActive()).isFalse();
            verify(policyRepository).save(policy);
            verify(auditLogService).log(any());
        }

        @Test
        @DisplayName("Throws ForbiddenException when policy belongs to a different store")
        void deletePolicy_wrongStore_throwsForbiddenException() {
            Store differentStore = Store.builder()
                    .id(888L)
                    .publicId(UUID.randomUUID())
                    .build();

            when(ownershipPolicy.resolveOwnStore(userId)).thenReturn(differentStore);
            when(policyRepository.findByPublicId(policyPublicId)).thenReturn(Optional.of(policy));

            assertThatThrownBy(() -> service.deletePolicy(userId, policyPublicId))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("does not belong");

            verify(policyRepository, never()).save(any());
        }

        @Test
        @DisplayName("Throws StorePolicyNotFoundException when policy does not exist")
        void deletePolicy_policyNotFound_throwsStorePolicyNotFoundException() {
            when(ownershipPolicy.resolveOwnStore(userId)).thenReturn(store);
            when(policyRepository.findByPublicId(policyPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deletePolicy(userId, policyPublicId))
                    .isInstanceOf(StorePolicyNotFoundException.class)
                    .hasMessageContaining(policyPublicId.toString());
        }
    }
}

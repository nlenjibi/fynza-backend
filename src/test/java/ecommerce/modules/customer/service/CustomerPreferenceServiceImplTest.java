package ecommerce.modules.customer.service;

import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.customer.dto.request.CustomerPreferenceRequest;
import ecommerce.modules.customer.dto.response.CustomerPreferenceResponse;
import ecommerce.modules.customer.entity.Customer;
import ecommerce.modules.customer.entity.CustomerPreference;
import ecommerce.modules.customer.enums.CustomerStatus;
import ecommerce.modules.customer.mapper.CustomerMapper;
import ecommerce.modules.customer.policy.CustomerOwnershipPolicy;
import ecommerce.modules.customer.repository.CustomerPreferenceRepository;
import ecommerce.modules.customer.service.impl.CustomerPreferenceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerPreferenceServiceImpl Tests")
class CustomerPreferenceServiceImplTest {

    @Mock private CustomerPreferenceRepository preferenceRepository;
    @Mock private CustomerOwnershipPolicy ownershipPolicy;
    @Mock private CustomerMapper mapper;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private CustomerPreferenceServiceImpl service;

    private UUID userId;
    private Customer customer;
    private CustomerPreference existingPreference;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        customer = Customer.builder()
                .userId(userId)
                .customerNumber("CUS-000001")
                .status(CustomerStatus.ACTIVE)
                .build();
        setCustomerId(customer, 1L);

        existingPreference = CustomerPreference.builder()
                .customer(customer)
                .language("en")
                .currency("GHS")
                .marketingOptIn(false)
                .emailNotifications(true)
                .smsNotifications(false)
                .pushNotifications(true)
                .build();
    }

    // ── getMyPreferences() ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMyPreferences(userId)")
    class GetMyPreferences {

        @Test
        @DisplayName("Returns existing preference when one exists for the customer")
        void getMyPreferences_existingPreference_returnsExisting() {
            when(ownershipPolicy.resolveOwn(userId)).thenReturn(customer);
            when(preferenceRepository.findByCustomer_Id(1L)).thenReturn(Optional.of(existingPreference));

            CustomerPreferenceResponse expected = CustomerPreferenceResponse.builder()
                    .language("en")
                    .currency("GHS")
                    .build();
            when(mapper.toPreferenceResponse(existingPreference)).thenReturn(expected);

            CustomerPreferenceResponse result = service.getMyPreferences(userId);

            assertThat(result).isNotNull();
            assertThat(result.getLanguage()).isEqualTo("en");
            assertThat(result.getCurrency()).isEqualTo("GHS");
            verify(preferenceRepository, never()).save(any());
        }

        @Test
        @DisplayName("Creates and saves a new preference when none exists for the customer")
        void getMyPreferences_noExistingPreference_createsAndSavesNew() {
            when(ownershipPolicy.resolveOwn(userId)).thenReturn(customer);
            when(preferenceRepository.findByCustomer_Id(1L)).thenReturn(Optional.empty());
            when(preferenceRepository.save(any(CustomerPreference.class))).thenReturn(existingPreference);

            CustomerPreferenceResponse expected = CustomerPreferenceResponse.builder().language("en").build();
            when(mapper.toPreferenceResponse(existingPreference)).thenReturn(expected);

            CustomerPreferenceResponse result = service.getMyPreferences(userId);

            assertThat(result).isNotNull();
            verify(preferenceRepository).save(any(CustomerPreference.class));
        }
    }

    // ── updatePreferences() ────────────────────────────────────────────────────

    @Nested
    @DisplayName("updatePreferences(userId, request)")
    class UpdatePreferences {

        @Test
        @DisplayName("Applies all non-null fields from request to the preference entity")
        void updatePreferences_allNonNullFields_appliesAll() {
            CustomerPreferenceRequest request = new CustomerPreferenceRequest();
            request.setLanguage("fr");
            request.setCurrency("USD");
            request.setMarketingOptIn(true);
            request.setEmailNotifications(false);
            request.setSmsNotifications(true);
            request.setPushNotifications(false);

            when(ownershipPolicy.resolveOwn(userId)).thenReturn(customer);
            when(preferenceRepository.findByCustomer_Id(1L)).thenReturn(Optional.of(existingPreference));
            when(preferenceRepository.save(existingPreference)).thenReturn(existingPreference);

            CustomerPreferenceResponse expected = CustomerPreferenceResponse.builder()
                    .language("fr")
                    .currency("USD")
                    .marketingOptIn(true)
                    .build();
            when(mapper.toPreferenceResponse(existingPreference)).thenReturn(expected);

            CustomerPreferenceResponse result = service.updatePreferences(userId, request);

            assertThat(existingPreference.getLanguage()).isEqualTo("fr");
            assertThat(existingPreference.getCurrency()).isEqualTo("USD");
            assertThat(existingPreference.getMarketingOptIn()).isTrue();
            assertThat(existingPreference.getEmailNotifications()).isFalse();
            assertThat(existingPreference.getSmsNotifications()).isTrue();
            assertThat(existingPreference.getPushNotifications()).isFalse();

            verify(preferenceRepository).save(existingPreference);
            verify(auditLogService).log(any());
        }

        @Test
        @DisplayName("Skips null fields in request — existing values are preserved")
        void updatePreferences_nullFields_preservesExistingValues() {
            CustomerPreferenceRequest request = new CustomerPreferenceRequest();
            request.setLanguage("de");
            // All other fields are null — should remain unchanged

            when(ownershipPolicy.resolveOwn(userId)).thenReturn(customer);
            when(preferenceRepository.findByCustomer_Id(1L)).thenReturn(Optional.of(existingPreference));
            when(preferenceRepository.save(existingPreference)).thenReturn(existingPreference);
            when(mapper.toPreferenceResponse(existingPreference)).thenReturn(CustomerPreferenceResponse.builder().build());

            service.updatePreferences(userId, request);

            assertThat(existingPreference.getLanguage()).isEqualTo("de");
            assertThat(existingPreference.getCurrency()).isEqualTo("GHS");    // unchanged
            assertThat(existingPreference.getMarketingOptIn()).isFalse();     // unchanged
            assertThat(existingPreference.getEmailNotifications()).isTrue();  // unchanged
        }

        @Test
        @DisplayName("Creates preference if none exists before applying updates")
        void updatePreferences_noExistingPreference_createsAndUpdates() {
            CustomerPreferenceRequest request = new CustomerPreferenceRequest();
            request.setLanguage("es");

            when(ownershipPolicy.resolveOwn(userId)).thenReturn(customer);
            when(preferenceRepository.findByCustomer_Id(1L)).thenReturn(Optional.empty());
            when(preferenceRepository.save(any(CustomerPreference.class))).thenReturn(existingPreference);
            when(mapper.toPreferenceResponse(existingPreference)).thenReturn(CustomerPreferenceResponse.builder().build());

            service.updatePreferences(userId, request);

            // Should have saved twice: once to create, once to update
            verify(preferenceRepository, org.mockito.Mockito.times(2)).save(any(CustomerPreference.class));
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private static void setCustomerId(Customer c, Long id) {
        try {
            java.lang.reflect.Field field = Customer.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(c, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

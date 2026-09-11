package ecommerce.modules.customer.mapper;

import ecommerce.modules.customer.dto.response.CustomerAddressResponse;
import ecommerce.modules.customer.dto.response.CustomerDetailResponse;
import ecommerce.modules.customer.dto.response.CustomerPreferenceResponse;
import ecommerce.modules.customer.dto.response.CustomerResponse;
import ecommerce.modules.customer.dto.response.CustomerStatusHistoryResponse;
import ecommerce.modules.customer.entity.Customer;
import ecommerce.modules.customer.entity.CustomerAddress;
import ecommerce.modules.customer.entity.CustomerPreference;
import ecommerce.modules.customer.entity.CustomerStatusHistory;
import ecommerce.modules.customer.enums.CustomerAddressType;
import ecommerce.modules.customer.enums.CustomerStatus;
import ecommerce.modules.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerMapper Tests")
class CustomerMapperTest {

    @InjectMocks
    private CustomerMapper mapper;

    private UUID customerId;
    private UUID userId;
    private Customer customer;
    private User user;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        userId = UUID.randomUUID();

        customer = Customer.builder()
                .userId(userId)
                .customerNumber("CUS-000001")
                .status(CustomerStatus.ACTIVE)
                .build();
        setPublicId(customer, customerId);
        setCustomerTimestamps(customer);

        user = User.builder()
                .id(userId)
                .email("jane@example.com")
                .username("jane")
                .firstName("Jane")
                .lastName("Doe")
                .phone("+233201234567")
                .password("{noop}secret")
                .build();
    }

    // ── toResponse() ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toResponse(customer, user)")
    class ToResponse {

        @Test
        @DisplayName("Maps all core fields from Customer and User correctly")
        void toResponse_mapsAllCoreFields() {
            CustomerResponse response = mapper.toResponse(customer, user);

            assertThat(response.getId()).isEqualTo(customerId);
            assertThat(response.getCustomerNumber()).isEqualTo("CUS-000001");
            assertThat(response.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
            assertThat(response.getFirstName()).isEqualTo("Jane");
            assertThat(response.getLastName()).isEqualTo("Doe");
            assertThat(response.getEmail()).isEqualTo("jane@example.com");
            assertThat(response.getPhone()).isEqualTo("+233201234567");
            assertThat(response.getCreatedAt()).isNotNull();
            assertThat(response.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Returns null phone when user has no phone")
        void toResponse_nullPhone_returnsNullPhone() {
            user.setPhone(null);

            CustomerResponse response = mapper.toResponse(customer, user);

            assertThat(response.getPhone()).isNull();
        }
    }

    // ── toAddressResponse() ───────────────────────────────────────────────────

    @Nested
    @DisplayName("toAddressResponse(address)")
    class ToAddressResponse {

        @Test
        @DisplayName("Maps all address fields correctly")
        void toAddressResponse_mapsAllFields() {
            UUID addressPublicId = UUID.randomUUID();
            Instant now = Instant.now();

            CustomerAddress address = CustomerAddress.builder()
                    .customer(customer)
                    .recipientName("John Doe")
                    .phoneNumber("+233201234567")
                    .addressLine1("1 Main Street")
                    .addressLine2("Apt 2B")
                    .city("Accra")
                    .region("Greater Accra")
                    .country("Ghana")
                    .postalCode("GA-123-4567")
                    .latitude(new BigDecimal("5.6037"))
                    .longitude(new BigDecimal("-0.1870"))
                    .addressType(CustomerAddressType.HOME)
                    .isDefault(true)
                    .build();
            setAddressPublicId(address, addressPublicId);
            setAddressTimestamps(address, now);

            CustomerAddressResponse response = mapper.toAddressResponse(address);

            assertThat(response.getId()).isEqualTo(addressPublicId);
            assertThat(response.getRecipientName()).isEqualTo("John Doe");
            assertThat(response.getPhoneNumber()).isEqualTo("+233201234567");
            assertThat(response.getAddressLine1()).isEqualTo("1 Main Street");
            assertThat(response.getAddressLine2()).isEqualTo("Apt 2B");
            assertThat(response.getCity()).isEqualTo("Accra");
            assertThat(response.getRegion()).isEqualTo("Greater Accra");
            assertThat(response.getCountry()).isEqualTo("Ghana");
            assertThat(response.getPostalCode()).isEqualTo("GA-123-4567");
            assertThat(response.getLatitude()).isEqualByComparingTo(new BigDecimal("5.6037"));
            assertThat(response.getLongitude()).isEqualByComparingTo(new BigDecimal("-0.1870"));
            assertThat(response.getAddressType()).isEqualTo(CustomerAddressType.HOME);
            assertThat(response.getIsDefault()).isTrue();
            assertThat(response.getCreatedAt()).isEqualTo(now);
            assertThat(response.getUpdatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("Maps null optional fields (addressLine2, region, etc.) as null")
        void toAddressResponse_nullOptionalFields_remainNull() {
            CustomerAddress address = CustomerAddress.builder()
                    .customer(customer)
                    .recipientName("Minimal Address")
                    .addressLine1("42 Short Road")
                    .city("Kumasi")
                    .country("Ghana")
                    .addressType(CustomerAddressType.OTHER)
                    .isDefault(false)
                    .build();
            setAddressPublicId(address, UUID.randomUUID());

            CustomerAddressResponse response = mapper.toAddressResponse(address);

            assertThat(response.getAddressLine2()).isNull();
            assertThat(response.getRegion()).isNull();
            assertThat(response.getPostalCode()).isNull();
            assertThat(response.getLatitude()).isNull();
            assertThat(response.getLongitude()).isNull();
        }
    }

    // ── toPreferenceResponse() ────────────────────────────────────────────────

    @Nested
    @DisplayName("toPreferenceResponse(preference)")
    class ToPreferenceResponse {

        @Test
        @DisplayName("Maps all preference fields correctly")
        void toPreferenceResponse_mapsAllFields() {
            UUID prefId = UUID.randomUUID();
            Instant updated = Instant.now();

            CustomerPreference preference = CustomerPreference.builder()
                    .customer(customer)
                    .language("fr")
                    .currency("EUR")
                    .marketingOptIn(true)
                    .emailNotifications(true)
                    .smsNotifications(false)
                    .pushNotifications(true)
                    .build();
            setPreferencePublicId(preference, prefId);
            setPreferenceUpdatedAt(preference, updated);

            CustomerPreferenceResponse response = mapper.toPreferenceResponse(preference);

            assertThat(response.getId()).isEqualTo(prefId);
            assertThat(response.getLanguage()).isEqualTo("fr");
            assertThat(response.getCurrency()).isEqualTo("EUR");
            assertThat(response.getMarketingOptIn()).isTrue();
            assertThat(response.getEmailNotifications()).isTrue();
            assertThat(response.getSmsNotifications()).isFalse();
            assertThat(response.getPushNotifications()).isTrue();
            assertThat(response.getUpdatedAt()).isEqualTo(updated);
        }
    }

    // ── toDetailResponse() ────────────────────────────────────────────────────

    @Nested
    @DisplayName("toDetailResponse(customer, user, preference, addresses)")
    class ToDetailResponse {

        @Test
        @DisplayName("Includes nested preference and address list")
        void toDetailResponse_withPreferenceAndAddresses_includesNested() {
            CustomerPreference preference = CustomerPreference.builder()
                    .customer(customer)
                    .language("en")
                    .currency("GHS")
                    .build();
            setPreferencePublicId(preference, UUID.randomUUID());

            CustomerAddress address = CustomerAddress.builder()
                    .customer(customer)
                    .recipientName("Jane")
                    .addressLine1("10 Oak Ave")
                    .city("Accra")
                    .country("Ghana")
                    .addressType(CustomerAddressType.HOME)
                    .isDefault(true)
                    .build();
            setAddressPublicId(address, UUID.randomUUID());

            CustomerDetailResponse response = mapper.toDetailResponse(customer, user, preference, List.of(address));

            assertThat(response.getId()).isEqualTo(customerId);
            assertThat(response.getCustomerNumber()).isEqualTo("CUS-000001");
            assertThat(response.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
            assertThat(response.getFirstName()).isEqualTo("Jane");
            assertThat(response.getPreferences()).isNotNull();
            assertThat(response.getAddresses()).hasSize(1);
        }

        @Test
        @DisplayName("Returns null preferences when preference is null")
        void toDetailResponse_nullPreference_returnsNullPreferences() {
            CustomerDetailResponse response = mapper.toDetailResponse(customer, user, null, List.of());

            assertThat(response.getPreferences()).isNull();
            assertThat(response.getAddresses()).isEmpty();
        }
    }

    // ── toStatusHistoryResponse() ─────────────────────────────────────────────

    @Nested
    @DisplayName("toStatusHistoryResponse(history)")
    class ToStatusHistoryResponse {

        @Test
        @DisplayName("Maps all history fields correctly")
        void toStatusHistoryResponse_mapsAllFields() {
            UUID historyPublicId = UUID.randomUUID();
            Instant createdAt = Instant.now();
            Instant expiresAt = Instant.now().plusSeconds(3600);
            UUID changedBy = UUID.randomUUID();

            CustomerStatusHistory history = CustomerStatusHistory.builder()
                    .customerId(1L)
                    .previousStatus(CustomerStatus.ACTIVE)
                    .newStatus(CustomerStatus.SUSPENDED)
                    .reason("Spam reports")
                    .changedBy(changedBy)
                    .expiresAt(expiresAt)
                    .build();
            setHistoryPublicId(history, historyPublicId);
            setHistoryCreatedAt(history, createdAt);

            CustomerStatusHistoryResponse response = mapper.toStatusHistoryResponse(history);

            assertThat(response.getId()).isEqualTo(historyPublicId);
            assertThat(response.getPreviousStatus()).isEqualTo(CustomerStatus.ACTIVE);
            assertThat(response.getNewStatus()).isEqualTo(CustomerStatus.SUSPENDED);
            assertThat(response.getReason()).isEqualTo("Spam reports");
            assertThat(response.getChangedBy()).isEqualTo(changedBy);
            assertThat(response.getCreatedAt()).isEqualTo(createdAt);
            assertThat(response.getExpiresAt()).isEqualTo(expiresAt);
        }
    }

    // ── Reflection helpers ────────────────────────────────────────────────────

    private static void setPublicId(Customer c, UUID id) {
        setField(c, Customer.class, "publicId", id);
    }

    private static void setCustomerTimestamps(Customer c) {
        setField(c, Customer.class, "createdAt", Instant.now());
        setField(c, Customer.class, "updatedAt", Instant.now());
    }

    private static void setAddressPublicId(CustomerAddress a, UUID id) {
        setField(a, CustomerAddress.class, "publicId", id);
    }

    private static void setAddressTimestamps(CustomerAddress a, Instant t) {
        setField(a, CustomerAddress.class, "createdAt", t);
        setField(a, CustomerAddress.class, "updatedAt", t);
    }

    private static void setPreferencePublicId(CustomerPreference p, UUID id) {
        setField(p, CustomerPreference.class, "publicId", id);
    }

    private static void setPreferenceUpdatedAt(CustomerPreference p, Instant t) {
        setField(p, CustomerPreference.class, "updatedAt", t);
    }

    private static void setHistoryPublicId(CustomerStatusHistory h, UUID id) {
        setField(h, CustomerStatusHistory.class, "publicId", id);
    }

    private static void setHistoryCreatedAt(CustomerStatusHistory h, Instant t) {
        setField(h, CustomerStatusHistory.class, "createdAt", t);
    }

    private static void setField(Object target, Class<?> clazz, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

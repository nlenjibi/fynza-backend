package ecommerce.modules.customer.service;

import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.customer.dto.request.CustomerUpdateRequest;
import ecommerce.modules.customer.dto.response.CustomerDetailResponse;
import ecommerce.modules.customer.dto.response.CustomerResponse;
import ecommerce.modules.customer.dto.response.CustomerStatsResponse;
import ecommerce.modules.customer.entity.Customer;
import ecommerce.modules.customer.entity.CustomerDetailView;
import ecommerce.modules.customer.entity.CustomerPreference;
import ecommerce.modules.customer.entity.CustomerStatsView;
import ecommerce.modules.customer.enums.CustomerStatus;
import ecommerce.modules.customer.exception.CustomerAlreadyExistsException;
import ecommerce.modules.customer.exception.CustomerNotFoundException;
import ecommerce.modules.customer.mapper.CustomerMapper;
import ecommerce.modules.customer.policy.CustomerOwnershipPolicy;
import ecommerce.modules.customer.repository.CustomerAddressRepository;
import ecommerce.modules.customer.repository.CustomerDetailViewRepository;
import ecommerce.modules.customer.repository.CustomerPreferenceRepository;
import ecommerce.modules.customer.repository.CustomerRepository;
import ecommerce.modules.customer.repository.CustomerStatusHistoryRepository;
import ecommerce.modules.customer.repository.CustomerStatsViewRepository;
import ecommerce.modules.customer.repository.CustomerSummaryViewRepository;
import ecommerce.modules.customer.service.impl.CustomerServiceImpl;
import ecommerce.modules.user.entity.User;
import ecommerce.modules.user.repository.UserRepository;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerServiceImpl Tests")
class CustomerServiceImplTest {

    @Mock private CustomerRepository              customerRepository;
    @Mock private CustomerPreferenceRepository    preferenceRepository;
    @Mock private CustomerAddressRepository       addressRepository;
    @Mock private CustomerStatusHistoryRepository historyRepository;
    @Mock private CustomerSummaryViewRepository   summaryViewRepository;
    @Mock private CustomerDetailViewRepository    detailViewRepository;
    @Mock private CustomerStatsViewRepository     statsViewRepository;
    @Mock private UserRepository                  userRepository;
    @Mock private CustomerNumberService           customerNumberService;
    @Mock private CustomerMapper                  mapper;
    @Mock private AuditLogService                 auditLogService;
    @Mock private CustomerOwnershipPolicy         ownershipPolicy;

    @InjectMocks
    private CustomerServiceImpl service;

    private UUID userId;
    private UUID customerPublicId;
    private Customer customer;
    private User user;

    @BeforeEach
    void setUp() {
        userId           = UUID.randomUUID();
        customerPublicId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .email("john@example.com")
                .username("john")
                .firstName("John")
                .lastName("Doe")
                .password("{noop}secret")
                .build();

        customer = Customer.builder()
                .userId(userId)
                .customerNumber("CUS-000001")
                .status(CustomerStatus.ACTIVE)
                .build();
    }

    // ── provision() ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("provision(userId, email)")
    class Provision {

        @Test
        @DisplayName("Happy path — creates customer, preference, and returns CustomerResponse")
        void provision_happyPath_createsCustomerAndPreference() {
            when(customerRepository.existsByUserId(userId)).thenReturn(false);
            when(customerNumberService.generate()).thenReturn("CUS-000001");
            when(customerRepository.save(any(Customer.class))).thenReturn(customer);
            when(preferenceRepository.save(any(CustomerPreference.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            CustomerResponse expected = CustomerResponse.builder()
                    .customerNumber("CUS-000001")
                    .status(CustomerStatus.ACTIVE)
                    .build();
            when(mapper.toResponse(any(Customer.class), any(User.class))).thenReturn(expected);

            CustomerResponse result = service.provision(userId, "john@example.com");

            assertThat(result).isNotNull();
            assertThat(result.getCustomerNumber()).isEqualTo("CUS-000001");
            verify(customerRepository).save(any(Customer.class));
            verify(preferenceRepository).save(any(CustomerPreference.class));
            verify(auditLogService).log(any());
        }

        @Test
        @DisplayName("Throws CustomerAlreadyExistsException when customer already exists for userId")
        void provision_duplicate_throwsCustomerAlreadyExistsException() {
            when(customerRepository.existsByUserId(userId)).thenReturn(true);

            assertThatThrownBy(() -> service.provision(userId, "john@example.com"))
                    .isInstanceOf(CustomerAlreadyExistsException.class)
                    .hasMessageContaining(userId.toString());

            verify(customerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Sets customer status to ACTIVE at provisioning time")
        void provision_setsStatusToActive() {
            when(customerRepository.existsByUserId(userId)).thenReturn(false);
            when(customerNumberService.generate()).thenReturn("CUS-000002");
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
            when(customerRepository.save(customerCaptor.capture())).thenReturn(customer);
            when(preferenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toResponse(any(), any())).thenReturn(CustomerResponse.builder().build());

            service.provision(userId, "john@example.com");

            Customer saved = customerCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
            assertThat(saved.getUserId()).isEqualTo(userId);
        }
    }

    // ── getMyCustomer() ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMyCustomer(userId)")
    class GetMyCustomer {

        private CustomerDetailView detailView;

        @BeforeEach
        void setUpDetailView() {
            detailView = mock(CustomerDetailView.class);
            when(detailView.getId()).thenReturn(1L);
            when(detailView.getPublicId()).thenReturn(customerPublicId);
            when(detailView.getUserId()).thenReturn(userId);
            when(detailView.getCustomerNumber()).thenReturn("CUS-000001");
            when(detailView.getStatus()).thenReturn(CustomerStatus.ACTIVE);
        }

        @Test
        @DisplayName("Returns CustomerDetailResponse for the authenticated user's customer record")
        void getMyCustomer_returnsDetailResponse() {
            when(detailViewRepository.findByUserId(userId)).thenReturn(Optional.of(detailView));
            when(addressRepository.findByCustomer_IdAndIsActiveTrue(1L)).thenReturn(List.of());

            CustomerDetailResponse expected = CustomerDetailResponse.builder()
                    .customerNumber("CUS-000001")
                    .status(CustomerStatus.ACTIVE)
                    .build();
            when(mapper.toDetailResponse(any(CustomerDetailView.class), any())).thenReturn(expected);

            CustomerDetailResponse result = service.getMyCustomer(userId);

            assertThat(result).isNotNull();
            assertThat(result.getCustomerNumber()).isEqualTo("CUS-000001");
            verify(detailViewRepository).findByUserId(userId);
        }

        @Test
        @DisplayName("Throws CustomerNotFoundException when no customer record exists for userId")
        void getMyCustomer_notFound_throwsCustomerNotFoundException() {
            when(detailViewRepository.findByUserId(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getMyCustomer(userId))
                    .isInstanceOf(CustomerNotFoundException.class);
        }

        @Test
        @DisplayName("Fetches only active addresses for the resolved customer")
        void getMyCustomer_fetchesOnlyActiveAddresses() {
            when(detailViewRepository.findByUserId(userId)).thenReturn(Optional.of(detailView));
            when(addressRepository.findByCustomer_IdAndIsActiveTrue(1L)).thenReturn(List.of());
            when(mapper.toDetailResponse(any(CustomerDetailView.class), any()))
                    .thenReturn(CustomerDetailResponse.builder().build());

            service.getMyCustomer(userId);

            verify(addressRepository).findByCustomer_IdAndIsActiveTrue(1L);
        }
    }

    // ── updateMyCustomer() ────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateMyCustomer(userId, request)")
    class UpdateMyCustomer {

        @Test
        @DisplayName("Updates firstName, lastName, and phone on the User entity")
        void updateMyCustomer_updatesUserFields() {
            CustomerUpdateRequest request = new CustomerUpdateRequest();
            request.setFirstName("Jane");
            request.setLastName("Smith");
            request.setPhone("+233201234567");

            when(ownershipPolicy.resolveOwn(userId)).thenReturn(customer);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            CustomerResponse expected = CustomerResponse.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .build();
            when(mapper.toResponse(any(), any())).thenReturn(expected);

            CustomerResponse result = service.updateMyCustomer(userId, request);

            assertThat(result.getFirstName()).isEqualTo("Jane");
            assertThat(result.getLastName()).isEqualTo("Smith");
            verify(userRepository).save(user);
            verify(auditLogService).log(any());
        }

        @Test
        @DisplayName("Skips null fields — only non-null fields are applied")
        void updateMyCustomer_skipsNullFields() {
            CustomerUpdateRequest request = new CustomerUpdateRequest();
            request.setFirstName("Alice");
            // lastName and phone are null — should not be applied

            when(ownershipPolicy.resolveOwn(userId)).thenReturn(customer);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(mapper.toResponse(any(), any())).thenReturn(CustomerResponse.builder().build());

            service.updateMyCustomer(userId, request);

            assertThat(user.getFirstName()).isEqualTo("Alice");
            assertThat(user.getLastName()).isEqualTo("Doe"); // unchanged original
        }
    }

    // ── getCustomerStats() ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getCustomerStats()")
    class GetCustomerStats {

        @Test
        @DisplayName("Returns stats from view when the singleton row exists")
        void getCustomerStats_returnsStatsFromView() {
            CustomerStatsView statsView = mock(CustomerStatsView.class);
            when(statsViewRepository.findById(1)).thenReturn(Optional.of(statsView));

            CustomerStatsResponse expected = CustomerStatsResponse.builder()
                    .totalCustomers(100L)
                    .activeCustomers(80L)
                    .newCustomersThisMonth(10L)
                    .build();
            when(mapper.toStats(statsView)).thenReturn(expected);

            CustomerStatsResponse result = service.getCustomerStats();

            assertThat(result.getTotalCustomers()).isEqualTo(100L);
            assertThat(result.getActiveCustomers()).isEqualTo(80L);
        }

        @Test
        @DisplayName("Returns zero-filled stats when view is empty")
        void getCustomerStats_returnsZeroWhenViewEmpty() {
            when(statsViewRepository.findById(1)).thenReturn(Optional.empty());

            CustomerStatsResponse result = service.getCustomerStats();

            assertThat(result.getTotalCustomers()).isZero();
            assertThat(result.getActiveCustomers()).isZero();
            assertThat(result.getNewCustomersThisMonth()).isZero();
        }
    }
}

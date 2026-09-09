package ecommerce.modules.customer.service;

import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.customer.dto.request.CustomerStatusRequest;
import ecommerce.modules.customer.dto.response.CustomerResponse;
import ecommerce.modules.customer.entity.Customer;
import ecommerce.modules.customer.entity.CustomerStatusHistory;
import ecommerce.modules.customer.enums.CustomerStatus;
import ecommerce.modules.customer.exception.CustomerNotFoundException;
import ecommerce.modules.customer.exception.CustomerStatusTransitionException;
import ecommerce.modules.customer.mapper.CustomerMapper;
import ecommerce.modules.customer.repository.CustomerRepository;
import ecommerce.modules.customer.repository.CustomerStatusHistoryRepository;
import ecommerce.modules.customer.service.impl.CustomerStatusServiceImpl;
import ecommerce.modules.customer.validation.CustomerStatusTransitionValidator;
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
@DisplayName("CustomerStatusServiceImpl Tests")
class CustomerStatusServiceImplTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private CustomerStatusHistoryRepository historyRepository;
    @Mock private UserRepository userRepository;
    @Mock private CustomerMapper mapper;
    @Mock private AuditLogService auditLogService;
    @Mock private CustomerStatusTransitionValidator transitionValidator;

    @InjectMocks
    private CustomerStatusServiceImpl service;

    private UUID customerPublicId;
    private UUID actorId;
    private Customer customer;
    private User user;

    @BeforeEach
    void setUp() {
        customerPublicId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .email("admin@example.com")
                .username("admin")
                .firstName("Admin")
                .lastName("User")
                .password("{noop}secret")
                .build();

        customer = Customer.builder()
                .userId(userId)
                .customerNumber("CUS-000001")
                .status(CustomerStatus.ACTIVE)
                .build();
        setPublicId(customer, customerPublicId);

        when(customerRepository.findByPublicId(customerPublicId)).thenReturn(Optional.of(customer));
        when(userRepository.findById(customer.getUserId())).thenReturn(Optional.of(user));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);
        when(historyRepository.save(any(CustomerStatusHistory.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(Customer.class), any(User.class))).thenReturn(CustomerResponse.builder().build());
    }

    // ── suspendCustomer() ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("suspendCustomer(customerPublicId, actorId, request)")
    class SuspendCustomer {

        @Test
        @DisplayName("Happy path — changes status to SUSPENDED and records history")
        void suspendCustomer_happyPath_changesStatusAndRecordsHistory() {
            CustomerStatusRequest request = new CustomerStatusRequest();
            request.setReason("Policy violation");
            request.setExpiresAt(Instant.now().plusSeconds(3600));

            CustomerResponse result = service.suspendCustomer(customerPublicId, actorId, request);

            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.SUSPENDED);
            verify(transitionValidator).validate(CustomerStatus.ACTIVE, CustomerStatus.SUSPENDED);
            verify(customerRepository).save(customer);

            ArgumentCaptor<CustomerStatusHistory> historyCaptor =
                    ArgumentCaptor.forClass(CustomerStatusHistory.class);
            verify(historyRepository).save(historyCaptor.capture());
            CustomerStatusHistory history = historyCaptor.getValue();
            assertThat(history.getPreviousStatus()).isEqualTo(CustomerStatus.ACTIVE);
            assertThat(history.getNewStatus()).isEqualTo(CustomerStatus.SUSPENDED);
            assertThat(history.getReason()).isEqualTo("Policy violation");
            assertThat(history.getChangedBy()).isEqualTo(actorId);
            assertThat(history.getExpiresAt()).isNotNull();

            verify(auditLogService).log(any());
        }

        @Test
        @DisplayName("Throws CustomerStatusTransitionException when transition is invalid")
        void suspendCustomer_invalidTransition_throwsException() {
            customer.setStatus(CustomerStatus.BLOCKED);
            doThrow(new CustomerStatusTransitionException(CustomerStatus.BLOCKED, CustomerStatus.SUSPENDED))
                    .when(transitionValidator).validate(CustomerStatus.BLOCKED, CustomerStatus.SUSPENDED);

            CustomerStatusRequest request = new CustomerStatusRequest();
            request.setReason("Attempt to re-suspend blocked customer");

            assertThatThrownBy(() -> service.suspendCustomer(customerPublicId, actorId, request))
                    .isInstanceOf(CustomerStatusTransitionException.class)
                    .hasMessageContaining("BLOCKED")
                    .hasMessageContaining("SUSPENDED");
        }

        @Test
        @DisplayName("Throws CustomerNotFoundException when customer publicId not found")
        void suspendCustomer_notFound_throwsCustomerNotFoundException() {
            UUID unknownId = UUID.randomUUID();
            when(customerRepository.findByPublicId(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.suspendCustomer(unknownId, actorId, new CustomerStatusRequest()))
                    .isInstanceOf(CustomerNotFoundException.class);
        }
    }

    // ── activateCustomer() ────────────────────────────────────────────────────

    @Nested
    @DisplayName("activateCustomer(customerPublicId, actorId)")
    class ActivateCustomer {

        @Test
        @DisplayName("Happy path — changes status to ACTIVE from SUSPENDED and records history")
        void activateCustomer_fromSuspended_changesStatusAndRecordsHistory() {
            customer.setStatus(CustomerStatus.SUSPENDED);

            CustomerResponse result = service.activateCustomer(customerPublicId, actorId);

            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
            verify(transitionValidator).validate(CustomerStatus.SUSPENDED, CustomerStatus.ACTIVE);
            verify(customerRepository).save(customer);

            ArgumentCaptor<CustomerStatusHistory> historyCaptor =
                    ArgumentCaptor.forClass(CustomerStatusHistory.class);
            verify(historyRepository).save(historyCaptor.capture());
            CustomerStatusHistory history = historyCaptor.getValue();
            assertThat(history.getPreviousStatus()).isEqualTo(CustomerStatus.SUSPENDED);
            assertThat(history.getNewStatus()).isEqualTo(CustomerStatus.ACTIVE);
            assertThat(history.getReason()).isEqualTo("Administrative reactivation");

            verify(auditLogService).log(any());
        }

        @Test
        @DisplayName("Throws CustomerNotFoundException when customer publicId not found")
        void activateCustomer_notFound_throwsCustomerNotFoundException() {
            UUID unknownId = UUID.randomUUID();
            when(customerRepository.findByPublicId(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.activateCustomer(unknownId, actorId))
                    .isInstanceOf(CustomerNotFoundException.class);
        }

        @Test
        @DisplayName("Throws CustomerStatusTransitionException when activation is invalid (e.g. from BLOCKED)")
        void activateCustomer_invalidTransition_throwsException() {
            customer.setStatus(CustomerStatus.BLOCKED);
            doThrow(new CustomerStatusTransitionException(CustomerStatus.BLOCKED, CustomerStatus.ACTIVE))
                    .when(transitionValidator).validate(CustomerStatus.BLOCKED, CustomerStatus.ACTIVE);

            assertThatThrownBy(() -> service.activateCustomer(customerPublicId, actorId))
                    .isInstanceOf(CustomerStatusTransitionException.class);
        }
    }

    // ── blockCustomer() ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("blockCustomer(customerPublicId, actorId, request)")
    class BlockCustomer {

        @Test
        @DisplayName("Happy path — changes status to BLOCKED and records history")
        void blockCustomer_happyPath_changesStatusAndRecordsHistory() {
            CustomerStatusRequest request = new CustomerStatusRequest();
            request.setReason("Fraudulent activity");

            CustomerResponse result = service.blockCustomer(customerPublicId, actorId, request);

            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.BLOCKED);
            verify(transitionValidator).validate(CustomerStatus.ACTIVE, CustomerStatus.BLOCKED);
            verify(customerRepository).save(customer);

            ArgumentCaptor<CustomerStatusHistory> historyCaptor =
                    ArgumentCaptor.forClass(CustomerStatusHistory.class);
            verify(historyRepository).save(historyCaptor.capture());
            CustomerStatusHistory history = historyCaptor.getValue();
            assertThat(history.getPreviousStatus()).isEqualTo(CustomerStatus.ACTIVE);
            assertThat(history.getNewStatus()).isEqualTo(CustomerStatus.BLOCKED);
            assertThat(history.getReason()).isEqualTo("Fraudulent activity");
            assertThat(history.getExpiresAt()).isNull(); // block has no expiry

            verify(auditLogService).log(any());
        }

        @Test
        @DisplayName("Throws CustomerStatusTransitionException when blocking from DELETED")
        void blockCustomer_fromDeleted_throwsException() {
            customer.setStatus(CustomerStatus.DELETED);
            doThrow(new CustomerStatusTransitionException(CustomerStatus.DELETED, CustomerStatus.BLOCKED))
                    .when(transitionValidator).validate(CustomerStatus.DELETED, CustomerStatus.BLOCKED);

            assertThatThrownBy(() -> service.blockCustomer(customerPublicId, actorId, new CustomerStatusRequest()))
                    .isInstanceOf(CustomerStatusTransitionException.class);
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private static void setPublicId(Customer c, UUID publicId) {
        try {
            java.lang.reflect.Field field = Customer.class.getDeclaredField("publicId");
            field.setAccessible(true);
            field.set(c, publicId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

package ecommerce.modules.customer.scheduler;

import ecommerce.modules.customer.entity.Customer;
import ecommerce.modules.customer.entity.CustomerStatusHistory;
import ecommerce.modules.customer.enums.CustomerStatus;
import ecommerce.modules.customer.repository.CustomerRepository;
import ecommerce.modules.customer.repository.CustomerStatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerSuspensionExpiryScheduler Tests")
class CustomerSuspensionExpirySchedulerTest {

    @Mock
    private CustomerStatusHistoryRepository historyRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerSuspensionExpiryScheduler scheduler;

    // ── No expired suspensions ────────────────────────────────────────────────

    @Nested
    @DisplayName("When no expired suspensions exist")
    class NoExpiredSuspensionsTests {

        @Test
        @DisplayName("Should make no customer updates")
        void expireSuspensions_WhenNoneExpired_SavesNothing() {
            when(historyRepository.findExpiredSuspensions(eq(CustomerStatus.SUSPENDED), any(Instant.class)))
                    .thenReturn(List.of());

            scheduler.expireSuspensions();

            verify(customerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should not touch customerRepository.findById at all")
        void expireSuspensions_WhenNoneExpired_NeverLooksUpCustomer() {
            when(historyRepository.findExpiredSuspensions(eq(CustomerStatus.SUSPENDED), any(Instant.class)))
                    .thenReturn(List.of());

            scheduler.expireSuspensions();

            verify(customerRepository, never()).findById(any());
        }
    }

    // ── Single expired suspension ─────────────────────────────────────────────

    @Nested
    @DisplayName("When one expired suspension exists")
    class SingleExpiredSuspensionTests {

        @Test
        @DisplayName("Should set customer status to ACTIVE and save")
        void expireSuspensions_WithOneSuspended_ReactivatesCustomer() {
            Long customerId = 1L;
            CustomerStatusHistory history = CustomerStatusHistory.builder()
                    .customerId(customerId)
                    .newStatus(CustomerStatus.SUSPENDED)
                    .expiresAt(Instant.now().minusSeconds(60))
                    .build();

            Customer customer = Customer.builder()
                    .id(customerId)
                    .publicId(UUID.randomUUID())
                    .status(CustomerStatus.SUSPENDED)
                    .isActive(true)
                    .build();

            when(historyRepository.findExpiredSuspensions(eq(CustomerStatus.SUSPENDED), any(Instant.class)))
                    .thenReturn(List.of(history));
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(customerRepository.save(customer)).thenReturn(customer);

            scheduler.expireSuspensions();

            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
            verify(customerRepository).save(customer);
        }

        @Test
        @DisplayName("Should skip customer that is no longer SUSPENDED")
        void expireSuspensions_WhenCustomerAlreadyActive_DoesNotSave() {
            Long customerId = 2L;
            CustomerStatusHistory history = CustomerStatusHistory.builder()
                    .customerId(customerId)
                    .newStatus(CustomerStatus.SUSPENDED)
                    .expiresAt(Instant.now().minusSeconds(60))
                    .build();

            Customer customer = Customer.builder()
                    .id(customerId)
                    .publicId(UUID.randomUUID())
                    .status(CustomerStatus.ACTIVE) // already reactivated
                    .isActive(true)
                    .build();

            when(historyRepository.findExpiredSuspensions(eq(CustomerStatus.SUSPENDED), any(Instant.class)))
                    .thenReturn(List.of(history));
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

            scheduler.expireSuspensions();

            verify(customerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should skip when customer record is missing")
        void expireSuspensions_WhenCustomerMissing_DoesNotSave() {
            Long customerId = 3L;
            CustomerStatusHistory history = CustomerStatusHistory.builder()
                    .customerId(customerId)
                    .newStatus(CustomerStatus.SUSPENDED)
                    .expiresAt(Instant.now().minusSeconds(60))
                    .build();

            when(historyRepository.findExpiredSuspensions(eq(CustomerStatus.SUSPENDED), any(Instant.class)))
                    .thenReturn(List.of(history));
            when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

            scheduler.expireSuspensions();

            verify(customerRepository, never()).save(any());
        }
    }

    // ── Multiple expired suspensions ──────────────────────────────────────────

    @Nested
    @DisplayName("When multiple expired suspensions exist")
    class MultipleExpiredSuspensionsTests {

        @Test
        @DisplayName("Should reactivate each suspended customer independently")
        void expireSuspensions_WithMultipleSuspended_ReactivatesAll() {
            Long id1 = 10L;
            Long id2 = 11L;

            CustomerStatusHistory h1 = CustomerStatusHistory.builder()
                    .customerId(id1)
                    .newStatus(CustomerStatus.SUSPENDED)
                    .expiresAt(Instant.now().minusSeconds(120))
                    .build();
            CustomerStatusHistory h2 = CustomerStatusHistory.builder()
                    .customerId(id2)
                    .newStatus(CustomerStatus.SUSPENDED)
                    .expiresAt(Instant.now().minusSeconds(60))
                    .build();

            Customer c1 = Customer.builder().id(id1).publicId(UUID.randomUUID())
                    .status(CustomerStatus.SUSPENDED).isActive(true).build();
            Customer c2 = Customer.builder().id(id2).publicId(UUID.randomUUID())
                    .status(CustomerStatus.SUSPENDED).isActive(true).build();

            when(historyRepository.findExpiredSuspensions(eq(CustomerStatus.SUSPENDED), any(Instant.class)))
                    .thenReturn(List.of(h1, h2));
            when(customerRepository.findById(id1)).thenReturn(Optional.of(c1));
            when(customerRepository.findById(id2)).thenReturn(Optional.of(c2));
            when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

            scheduler.expireSuspensions();

            assertThat(c1.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
            assertThat(c2.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
            verify(customerRepository, times(2)).save(any(Customer.class));
        }
    }
}

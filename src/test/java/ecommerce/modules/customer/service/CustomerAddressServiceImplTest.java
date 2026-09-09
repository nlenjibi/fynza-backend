package ecommerce.modules.customer.service;

import ecommerce.common.exception.BadRequestException;
import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.customer.dto.request.CustomerAddressRequest;
import ecommerce.modules.customer.dto.response.CustomerAddressResponse;
import ecommerce.modules.customer.entity.Customer;
import ecommerce.modules.customer.entity.CustomerAddress;
import ecommerce.modules.customer.enums.CustomerAddressType;
import ecommerce.modules.customer.enums.CustomerStatus;
import ecommerce.modules.customer.exception.AddressNotFoundException;
import ecommerce.modules.customer.exception.AddressOwnershipException;
import ecommerce.modules.customer.mapper.CustomerMapper;
import ecommerce.modules.customer.policy.CustomerOwnershipPolicy;
import ecommerce.modules.customer.repository.CustomerAddressRepository;
import ecommerce.modules.customer.service.impl.CustomerAddressServiceImpl;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerAddressServiceImpl Tests")
class CustomerAddressServiceImplTest {

    @Mock private CustomerAddressRepository addressRepository;
    @Mock private CustomerOwnershipPolicy ownershipPolicy;
    @Mock private CustomerMapper mapper;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private CustomerAddressServiceImpl service;

    private UUID userId;
    private UUID addressPublicId;
    private Customer customer;
    private CustomerAddress address;
    private CustomerAddressRequest validRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        addressPublicId = UUID.randomUUID();

        customer = Customer.builder()
                .userId(userId)
                .customerNumber("CUS-000001")
                .status(CustomerStatus.ACTIVE)
                .build();
        // Simulate a persisted customer with id=1L
        setCustomerId(customer, 1L);

        address = CustomerAddress.builder()
                .customer(customer)
                .recipientName("John Doe")
                .addressLine1("1 Main St")
                .city("Accra")
                .country("Ghana")
                .addressType(CustomerAddressType.HOME)
                .isDefault(false)
                .isActive(true)
                .build();
        setAddressPublicId(address, addressPublicId);
        setAddressId(address, 10L);

        validRequest = new CustomerAddressRequest();
        validRequest.setRecipientName("John Doe");
        validRequest.setAddressLine1("1 Main St");
        validRequest.setCity("Accra");
        validRequest.setCountry("Ghana");
    }

    // ── addAddress() ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addAddress(userId, request)")
    class AddAddress {

        @Test
        @DisplayName("Happy path — saves and returns address response")
        void addAddress_happyPath_savesAndReturnsResponse() {
            when(ownershipPolicy.resolveOwn(userId)).thenReturn(customer);
            when(addressRepository.countByCustomer_IdAndIsActiveTrue(1L)).thenReturn(0);
            when(addressRepository.save(any(CustomerAddress.class))).thenReturn(address);

            CustomerAddressResponse expected = CustomerAddressResponse.builder()
                    .recipientName("John Doe")
                    .city("Accra")
                    .build();
            when(mapper.toAddressResponse(address)).thenReturn(expected);

            CustomerAddressResponse result = service.addAddress(userId, validRequest);

            assertThat(result).isNotNull();
            assertThat(result.getRecipientName()).isEqualTo("John Doe");
            verify(addressRepository).save(any(CustomerAddress.class));
            verify(auditLogService).log(any());
        }

        @Test
        @DisplayName("Throws BadRequestException when address count is at maximum (10)")
        void addAddress_atMaximum_throwsBadRequestException() {
            when(ownershipPolicy.resolveOwn(userId)).thenReturn(customer);
            when(addressRepository.countByCustomer_IdAndIsActiveTrue(1L)).thenReturn(10);

            assertThatThrownBy(() -> service.addAddress(userId, validRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("10");

            verify(addressRepository, never()).save(any());
        }

        @Test
        @DisplayName("Clears default flag on existing addresses when new address is marked as default")
        void addAddress_withIsDefault_clearsExistingDefault() {
            validRequest.setIsDefault(true);

            when(ownershipPolicy.resolveOwn(userId)).thenReturn(customer);
            when(addressRepository.countByCustomer_IdAndIsActiveTrue(1L)).thenReturn(3);
            when(addressRepository.save(any(CustomerAddress.class))).thenReturn(address);
            when(mapper.toAddressResponse(address)).thenReturn(CustomerAddressResponse.builder().build());

            service.addAddress(userId, validRequest);

            verify(addressRepository).clearDefaultByCustomerId(1L);
        }

        @Test
        @DisplayName("Does NOT clear default when isDefault is false")
        void addAddress_withIsDefaultFalse_doesNotClearDefault() {
            validRequest.setIsDefault(false);

            when(ownershipPolicy.resolveOwn(userId)).thenReturn(customer);
            when(addressRepository.countByCustomer_IdAndIsActiveTrue(1L)).thenReturn(0);
            when(addressRepository.save(any(CustomerAddress.class))).thenReturn(address);
            when(mapper.toAddressResponse(address)).thenReturn(CustomerAddressResponse.builder().build());

            service.addAddress(userId, validRequest);

            verify(addressRepository, never()).clearDefaultByCustomerId(any());
        }

        @Test
        @DisplayName("Defaults addressType to HOME when none provided in request")
        void addAddress_nullAddressType_defaultsToHome() {
            validRequest.setAddressType(null);

            when(ownershipPolicy.resolveOwn(userId)).thenReturn(customer);
            when(addressRepository.countByCustomer_IdAndIsActiveTrue(1L)).thenReturn(0);

            org.mockito.ArgumentCaptor<CustomerAddress> captor =
                    org.mockito.ArgumentCaptor.forClass(CustomerAddress.class);
            when(addressRepository.save(captor.capture())).thenReturn(address);
            when(mapper.toAddressResponse(address)).thenReturn(CustomerAddressResponse.builder().build());

            service.addAddress(userId, validRequest);

            assertThat(captor.getValue().getAddressType()).isEqualTo(CustomerAddressType.HOME);
        }
    }

    // ── updateAddress() ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateAddress(userId, addressPublicId, request)")
    class UpdateAddress {

        @Test
        @DisplayName("Happy path — applies updates and returns updated response")
        void updateAddress_happyPath_appliesUpdatesAndReturns() {
            CustomerAddressRequest request = new CustomerAddressRequest();
            request.setCity("Kumasi");

            when(ownershipPolicy.resolveOwn(userId)).thenReturn(customer);
            when(addressRepository.findByPublicId(addressPublicId)).thenReturn(Optional.of(address));
            when(addressRepository.save(address)).thenReturn(address);

            CustomerAddressResponse expected = CustomerAddressResponse.builder().city("Kumasi").build();
            when(mapper.toAddressResponse(address)).thenReturn(expected);

            CustomerAddressResponse result = service.updateAddress(userId, addressPublicId, request);

            assertThat(result.getCity()).isEqualTo("Kumasi");
            verify(addressRepository).save(address);
            verify(auditLogService).log(any());
        }

        @Test
        @DisplayName("Throws AddressNotFoundException when address publicId not found")
        void updateAddress_notFound_throwsAddressNotFoundException() {
            when(ownershipPolicy.resolveOwn(userId)).thenReturn(customer);
            when(addressRepository.findByPublicId(addressPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateAddress(userId, addressPublicId, validRequest))
                    .isInstanceOf(AddressNotFoundException.class)
                    .hasMessageContaining(addressPublicId.toString());
        }

        @Test
        @DisplayName("Throws AddressNotFoundException when address is inactive")
        void updateAddress_inactive_throwsAddressNotFoundException() {
            address.setIsActive(false);

            when(ownershipPolicy.resolveOwn(userId)).thenReturn(customer);
            when(addressRepository.findByPublicId(addressPublicId)).thenReturn(Optional.of(address));

            assertThatThrownBy(() -> service.updateAddress(userId, addressPublicId, validRequest))
                    .isInstanceOf(AddressNotFoundException.class);
        }

        @Test
        @DisplayName("Throws AddressOwnershipException when address belongs to a different customer")
        void updateAddress_ownershipViolation_throwsAddressOwnershipException() {
            Customer otherCustomer = Customer.builder()
                    .userId(UUID.randomUUID())
                    .customerNumber("CUS-999999")
                    .status(CustomerStatus.ACTIVE)
                    .build();
            setCustomerId(otherCustomer, 999L);
            address.setCustomer(otherCustomer);

            when(ownershipPolicy.resolveOwn(userId)).thenReturn(customer);
            when(addressRepository.findByPublicId(addressPublicId)).thenReturn(Optional.of(address));

            assertThatThrownBy(() -> service.updateAddress(userId, addressPublicId, validRequest))
                    .isInstanceOf(AddressOwnershipException.class);
        }
    }

    // ── deleteAddress() ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteAddress(userId, addressPublicId)")
    class DeleteAddress {

        @Test
        @DisplayName("Soft-deletes the address by setting isActive=false")
        void deleteAddress_setsIsActiveFalse() {
            when(ownershipPolicy.resolveOwn(userId)).thenReturn(customer);
            when(addressRepository.findByPublicId(addressPublicId)).thenReturn(Optional.of(address));
            when(addressRepository.save(address)).thenReturn(address);

            service.deleteAddress(userId, addressPublicId);

            assertThat(address.getIsActive()).isFalse();
            verify(addressRepository).save(address);
            verify(auditLogService).log(any());
        }

        @Test
        @DisplayName("Clears isDefault flag when deleting a default address")
        void deleteAddress_defaultAddress_clearsDefaultFlag() {
            address.setIsDefault(true);

            when(ownershipPolicy.resolveOwn(userId)).thenReturn(customer);
            when(addressRepository.findByPublicId(addressPublicId)).thenReturn(Optional.of(address));
            when(addressRepository.save(address)).thenReturn(address);

            service.deleteAddress(userId, addressPublicId);

            assertThat(address.getIsDefault()).isFalse();
            assertThat(address.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("Does not alter isDefault when address is not the default")
        void deleteAddress_nonDefaultAddress_doesNotAlterIsDefault() {
            address.setIsDefault(false);

            when(ownershipPolicy.resolveOwn(userId)).thenReturn(customer);
            when(addressRepository.findByPublicId(addressPublicId)).thenReturn(Optional.of(address));
            when(addressRepository.save(address)).thenReturn(address);

            service.deleteAddress(userId, addressPublicId);

            assertThat(address.getIsDefault()).isFalse();
        }
    }

    // ── setDefaultAddress() ───────────────────────────────────────────────────

    @Nested
    @DisplayName("setDefaultAddress(userId, addressPublicId)")
    class SetDefaultAddress {

        @Test
        @DisplayName("Clears existing defaults, sets the target address as default, and returns response")
        void setDefaultAddress_setsAddressAsDefault() {
            when(ownershipPolicy.resolveOwn(userId)).thenReturn(customer);
            when(addressRepository.findByPublicId(addressPublicId)).thenReturn(Optional.of(address));
            when(addressRepository.save(address)).thenReturn(address);

            CustomerAddressResponse expected = CustomerAddressResponse.builder().isDefault(true).build();
            when(mapper.toAddressResponse(address)).thenReturn(expected);

            CustomerAddressResponse result = service.setDefaultAddress(userId, addressPublicId);

            assertThat(address.getIsDefault()).isTrue();
            assertThat(result.getIsDefault()).isTrue();
            verify(addressRepository).clearDefaultByCustomerId(1L);
            verify(addressRepository).save(address);
        }

        @Test
        @DisplayName("Throws AddressNotFoundException when address publicId not found")
        void setDefaultAddress_notFound_throwsAddressNotFoundException() {
            when(ownershipPolicy.resolveOwn(userId)).thenReturn(customer);
            when(addressRepository.findByPublicId(addressPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.setDefaultAddress(userId, addressPublicId))
                    .isInstanceOf(AddressNotFoundException.class);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void setCustomerId(Customer c, Long id) {
        try {
            java.lang.reflect.Field field = Customer.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(c, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setAddressId(CustomerAddress a, Long id) {
        try {
            java.lang.reflect.Field field = CustomerAddress.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(a, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setAddressPublicId(CustomerAddress a, UUID publicId) {
        try {
            java.lang.reflect.Field field = CustomerAddress.class.getDeclaredField("publicId");
            field.setAccessible(true);
            field.set(a, publicId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

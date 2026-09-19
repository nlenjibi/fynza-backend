package ecommerce.modules.inventory.service;

import ecommerce.modules.inventory.dto.request.CreateInventoryLocationRequest;
import ecommerce.modules.inventory.dto.response.InventoryLocationResponse;
import ecommerce.modules.inventory.entity.InventoryLocation;
import ecommerce.modules.inventory.enums.LocationType;
import ecommerce.modules.inventory.exception.InventoryNotFoundException;
import ecommerce.modules.inventory.exception.InventoryOwnershipException;
import ecommerce.modules.inventory.exception.InvalidInventoryOperationException;
import ecommerce.modules.inventory.repository.InventoryLocationRepository;
import ecommerce.modules.inventory.service.impl.InventoryLocationServiceImpl;
import ecommerce.modules.seller.entity.Seller;
import ecommerce.modules.seller.repository.SellerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
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
@DisplayName("InventoryLocationServiceImpl")
class InventoryLocationServiceImplTest {

    @Mock private InventoryLocationRepository locationRepository;
    @Mock private SellerRepository            sellerRepository;

    @InjectMocks
    private InventoryLocationServiceImpl service;

    private UUID userId;
    private UUID locationPublicId;
    private Seller seller;
    private InventoryLocation location;

    @BeforeEach
    void setUp() {
        userId          = UUID.randomUUID();
        locationPublicId = UUID.randomUUID();

        seller = Seller.builder()
                .ownerUserId(userId)
                .displayName("Test Seller")
                .build();
        setField(seller, "id", 1L);

        location = InventoryLocation.builder()
                .sellerId(1L)
                .name("Main Warehouse")
                .code("WH-001")
                .locationType(LocationType.WAREHOUSE)
                .build();
        setField(location, "id", 10L);
        setField(location, "publicId", locationPublicId);
        setField(location, "isActive", true);
        setField(location, "status", "ACTIVE");
    }

    // ─── createLocation ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("createLocation")
    class CreateLocation {

        @Test
        @DisplayName("happy path — saves location and returns response")
        void createLocation_happyPath_savesAndReturnsResponse() {
            CreateInventoryLocationRequest request = new CreateInventoryLocationRequest();
            request.setName("Main Warehouse");
            request.setCode("WH-001");
            request.setLocationType(LocationType.WAREHOUSE);

            when(sellerRepository.findByOwnerUserId(userId)).thenReturn(Optional.of(seller));
            when(locationRepository.existsBySellerIdAndCode(1L, "WH-001")).thenReturn(false);
            when(locationRepository.save(any(InventoryLocation.class))).thenReturn(location);

            InventoryLocationResponse result = service.createLocation(request, userId);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Main Warehouse");
            assertThat(result.getCode()).isEqualTo("WH-001");
            verify(locationRepository).save(any(InventoryLocation.class));
        }

        @Test
        @DisplayName("throws InvalidInventoryOperationException when location code already exists for seller")
        void createLocation_duplicateCode_throwsInvalidInventoryOperationException() {
            CreateInventoryLocationRequest request = new CreateInventoryLocationRequest();
            request.setName("Duplicate Warehouse");
            request.setCode("WH-001");
            request.setLocationType(LocationType.WAREHOUSE);

            when(sellerRepository.findByOwnerUserId(userId)).thenReturn(Optional.of(seller));
            when(locationRepository.existsBySellerIdAndCode(1L, "WH-001")).thenReturn(true);

            assertThatThrownBy(() -> service.createLocation(request, userId))
                    .isInstanceOf(InvalidInventoryOperationException.class)
                    .hasMessageContaining("WH-001")
                    .hasMessageContaining("already exists");

            verify(locationRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws InventoryOwnershipException when no seller account exists for user")
        void createLocation_noSellerAccount_throwsInventoryOwnershipException() {
            when(sellerRepository.findByOwnerUserId(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createLocation(new CreateInventoryLocationRequest(), userId))
                    .isInstanceOf(InventoryOwnershipException.class)
                    .hasMessageContaining("No seller account found");

            verify(locationRepository, never()).save(any());
        }

        @Test
        @DisplayName("creates STORE type location when requested")
        void createLocation_storeType_savesWithCorrectLocationType() {
            CreateInventoryLocationRequest request = new CreateInventoryLocationRequest();
            request.setName("Downtown Store");
            request.setCode("ST-001");
            request.setLocationType(LocationType.STORE);

            InventoryLocation storeLocation = InventoryLocation.builder()
                    .sellerId(1L)
                    .name("Downtown Store")
                    .code("ST-001")
                    .locationType(LocationType.STORE)
                    .build();
            setField(storeLocation, "publicId", UUID.randomUUID());

            when(sellerRepository.findByOwnerUserId(userId)).thenReturn(Optional.of(seller));
            when(locationRepository.existsBySellerIdAndCode(1L, "ST-001")).thenReturn(false);
            when(locationRepository.save(any(InventoryLocation.class))).thenReturn(storeLocation);

            InventoryLocationResponse result = service.createLocation(request, userId);

            assertThat(result.getLocationType()).isEqualTo(LocationType.STORE);
        }
    }

    // ─── updateLocation ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateLocation")
    class UpdateLocation {

        @Test
        @DisplayName("happy path — name and locationType updated, saves and returns response")
        void updateLocation_happyPath_updatesAndReturnsResponse() {
            CreateInventoryLocationRequest request = new CreateInventoryLocationRequest();
            request.setName("Updated Warehouse");
            request.setLocationType(LocationType.FULFILLMENT_CENTER);

            when(sellerRepository.findByOwnerUserId(userId)).thenReturn(Optional.of(seller));
            when(locationRepository.findByPublicId(locationPublicId)).thenReturn(Optional.of(location));
            when(locationRepository.save(location)).thenReturn(location);

            InventoryLocationResponse result = service.updateLocation(locationPublicId, request, userId);

            assertThat(result).isNotNull();
            assertThat(location.getName()).isEqualTo("Updated Warehouse");
            assertThat(location.getLocationType()).isEqualTo(LocationType.FULFILLMENT_CENTER);
            verify(locationRepository).save(location);
        }

        @Test
        @DisplayName("updates storeId when provided in request")
        void updateLocation_withStoreId_updatesStoreId() {
            CreateInventoryLocationRequest request = new CreateInventoryLocationRequest();
            request.setName("Store Warehouse");
            request.setLocationType(LocationType.WAREHOUSE);
            request.setStoreId(42L);

            when(sellerRepository.findByOwnerUserId(userId)).thenReturn(Optional.of(seller));
            when(locationRepository.findByPublicId(locationPublicId)).thenReturn(Optional.of(location));
            when(locationRepository.save(location)).thenReturn(location);

            service.updateLocation(locationPublicId, request, userId);

            assertThat(location.getStoreId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("throws InventoryNotFoundException when location does not exist")
        void updateLocation_notFound_throwsInventoryNotFoundException() {
            when(sellerRepository.findByOwnerUserId(userId)).thenReturn(Optional.of(seller));
            when(locationRepository.findByPublicId(locationPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateLocation(locationPublicId, new CreateInventoryLocationRequest(), userId))
                    .isInstanceOf(InventoryNotFoundException.class)
                    .hasMessageContaining(locationPublicId.toString());
        }

        @Test
        @DisplayName("throws InventoryOwnershipException when location belongs to a different seller")
        void updateLocation_wrongOwner_throwsInventoryOwnershipException() {
            InventoryLocation otherSellerLocation = InventoryLocation.builder()
                    .sellerId(999L)
                    .name("Other Store")
                    .code("OS-001")
                    .build();
            setField(otherSellerLocation, "publicId", locationPublicId);

            when(sellerRepository.findByOwnerUserId(userId)).thenReturn(Optional.of(seller));
            when(locationRepository.findByPublicId(locationPublicId)).thenReturn(Optional.of(otherSellerLocation));

            assertThatThrownBy(() -> service.updateLocation(locationPublicId, new CreateInventoryLocationRequest(), userId))
                    .isInstanceOf(InventoryOwnershipException.class)
                    .hasMessageContaining("does not belong");

            verify(locationRepository, never()).save(any());
        }
    }

    // ─── getSellerLocations ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getSellerLocations")
    class GetSellerLocations {

        @Test
        @DisplayName("happy path — returns all active locations for the seller")
        void getSellerLocations_happyPath_returnsActiveLocations() {
            InventoryLocation location2 = InventoryLocation.builder()
                    .sellerId(1L)
                    .name("Secondary Warehouse")
                    .code("WH-002")
                    .locationType(LocationType.WAREHOUSE)
                    .build();
            setField(location2, "publicId", UUID.randomUUID());
            setField(location2, "isActive", true);

            when(sellerRepository.findByOwnerUserId(userId)).thenReturn(Optional.of(seller));
            when(locationRepository.findBySellerIdAndIsActiveTrue(1L))
                    .thenReturn(List.of(location, location2));

            List<InventoryLocationResponse> result = service.getSellerLocations(userId);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(InventoryLocationResponse::getCode)
                    .containsExactlyInAnyOrder("WH-001", "WH-002");
        }

        @Test
        @DisplayName("returns empty list when seller has no active locations")
        void getSellerLocations_noLocations_returnsEmptyList() {
            when(sellerRepository.findByOwnerUserId(userId)).thenReturn(Optional.of(seller));
            when(locationRepository.findBySellerIdAndIsActiveTrue(1L)).thenReturn(List.of());

            List<InventoryLocationResponse> result = service.getSellerLocations(userId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("throws InventoryOwnershipException when no seller account exists for user")
        void getSellerLocations_noSellerAccount_throwsInventoryOwnershipException() {
            when(sellerRepository.findByOwnerUserId(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getSellerLocations(userId))
                    .isInstanceOf(InventoryOwnershipException.class)
                    .hasMessageContaining("No seller account found");
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Class<?> clazz = target.getClass();
            while (clazz != null) {
                try {
                    Field field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    field.set(target, value);
                    return;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            throw new RuntimeException("Field not found: " + fieldName);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}

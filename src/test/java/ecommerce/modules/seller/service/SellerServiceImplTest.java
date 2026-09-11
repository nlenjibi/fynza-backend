package ecommerce.modules.seller.service;

import ecommerce.common.enums.SellerStatus;
import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.seller.dto.request.UpdateSellerRequest;
import ecommerce.modules.seller.dto.response.SellerResponse;
import ecommerce.modules.seller.entity.Seller;
import ecommerce.modules.seller.exception.SellerAlreadyExistsException;
import ecommerce.modules.seller.mapper.SellerMapper;
import ecommerce.modules.seller.policy.SellerOwnershipPolicy;
import ecommerce.modules.seller.repository.SellerBusinessRepository;
import ecommerce.modules.seller.repository.SellerRepository;
import ecommerce.modules.seller.repository.SellerStatusHistoryRepository;
import ecommerce.modules.seller.repository.SellerVerificationRepository;
import ecommerce.modules.seller.service.impl.SellerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SellerServiceImpl Tests")
class SellerServiceImplTest {

    @Mock private SellerRepository              sellerRepository;
    @Mock private SellerBusinessRepository      businessRepository;
    @Mock private SellerStatusHistoryRepository historyRepository;
    @Mock private SellerVerificationRepository  verificationRepository;
    @Mock private SellerNumberService           sellerNumberService;
    @Mock private SellerMapper                  mapper;
    @Mock private AuditLogService               auditLogService;
    @Mock private SellerOwnershipPolicy         ownershipPolicy;

    @InjectMocks
    private SellerServiceImpl service;

    private UUID ownerUserId;
    private UUID sellerPublicId;
    private Seller seller;

    @BeforeEach
    void setUp() {
        ownerUserId    = UUID.randomUUID();
        sellerPublicId = UUID.randomUUID();

        seller = Seller.builder()
                .sellerNumber("SEL-000001")
                .ownerUserId(ownerUserId)
                .status(SellerStatus.DRAFT)
                .displayName("Test Store")
                .build();
        setId(seller, 1L);
        setPublicId(seller, sellerPublicId);

        when(sellerRepository.save(any(Seller.class))).thenReturn(seller);
        when(sellerNumberService.formatFromId(1L)).thenReturn("SEL-000001");
        when(businessRepository.findBySellerId(seller.getId())).thenReturn(Optional.empty());
        when(verificationRepository.findBySellerId(seller.getId())).thenReturn(Collections.emptyList());
        when(mapper.toResponse(any(Seller.class))).thenReturn(SellerResponse.builder().sellerNumber("SEL-000001").build());
    }

    // ── provision() ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("provision(ownerUserId, displayName)")
    class Provision {

        @Test
        @DisplayName("Happy path — creates Seller, formats sellerNumber via service")
        void provision_happyPath_createsSellerAndFormatsNumber() {
            when(sellerRepository.existsByOwnerUserId(ownerUserId)).thenReturn(false);

            SellerResponse result = service.provision(ownerUserId, "Test Store");

            assertThat(result).isNotNull();
            verify(sellerRepository).existsByOwnerUserId(ownerUserId);
            verify(sellerNumberService).formatFromId(any(Long.class));
            verify(auditLogService).log(any());
        }

        @Test
        @DisplayName("Throws SellerAlreadyExistsException when seller already exists")
        void provision_alreadyExists_throws() {
            when(sellerRepository.existsByOwnerUserId(ownerUserId)).thenReturn(true);

            assertThatThrownBy(() -> service.provision(ownerUserId, "Test Store"))
                    .isInstanceOf(SellerAlreadyExistsException.class)
                    .hasMessageContaining(ownerUserId.toString());

            verify(sellerRepository, never()).save(any());
        }
    }

    // ── updateMySeller() ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateMySeller(userId, request)")
    class UpdateMySeller {

        @Test
        @DisplayName("Happy path — updates displayName and audits")
        void updateMySeller_happyPath_updatesAndAudits() {
            when(ownershipPolicy.resolveOwn(ownerUserId)).thenReturn(seller);

            UpdateSellerRequest request = new UpdateSellerRequest();
            request.setDisplayName("New Store Name");

            service.updateMySeller(ownerUserId, request);

            assertThat(seller.getDisplayName()).isEqualTo("New Store Name");
            verify(sellerRepository).save(seller);
            verify(auditLogService).log(any());
        }

        @Test
        @DisplayName("Null displayName — preserves existing value")
        void updateMySeller_nullDisplayName_preservesExisting() {
            when(ownershipPolicy.resolveOwn(ownerUserId)).thenReturn(seller);

            UpdateSellerRequest request = new UpdateSellerRequest();

            service.updateMySeller(ownerUserId, request);

            assertThat(seller.getDisplayName()).isEqualTo("Test Store");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void setPublicId(Seller s, UUID publicId) {
        try {
            java.lang.reflect.Field field = Seller.class.getDeclaredField("publicId");
            field.setAccessible(true);
            field.set(s, publicId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setId(Seller s, Long id) {
        try {
            java.lang.reflect.Field field = Seller.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(s, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

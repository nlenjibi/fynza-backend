package ecommerce.modules.pricing.service;

import ecommerce.common.event.FynzaEventPublisher;
import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.pricing.dto.request.CreatePriceRequest;
import ecommerce.modules.pricing.dto.request.CreatePriceTierRequest;
import ecommerce.modules.pricing.dto.request.SchedulePriceRequest;
import ecommerce.modules.pricing.dto.request.UpdatePriceRequest;
import ecommerce.modules.pricing.dto.response.PriceResponse;
import ecommerce.modules.pricing.dto.response.PriceTierResponse;
import ecommerce.modules.pricing.entity.Price;
import ecommerce.modules.pricing.entity.PriceList;
import ecommerce.modules.pricing.entity.PriceTier;
import ecommerce.modules.pricing.enums.PriceStatus;
import ecommerce.modules.pricing.enums.SupportedCurrency;
import ecommerce.modules.pricing.exception.InvalidPriceException;
import ecommerce.modules.pricing.exception.OverlappingPriceException;
import ecommerce.modules.pricing.exception.PriceListNotFoundException;
import ecommerce.modules.pricing.exception.PriceNotFoundException;
import ecommerce.modules.pricing.exception.PriceOwnershipException;
import ecommerce.modules.pricing.mapper.PriceMapper;
import ecommerce.modules.pricing.repository.PriceHistoryRepository;
import ecommerce.modules.pricing.repository.PriceListRepository;
import ecommerce.modules.pricing.repository.PriceRepository;
import ecommerce.modules.pricing.repository.PriceTierRepository;
import ecommerce.modules.pricing.service.impl.PriceServiceImpl;
import ecommerce.modules.pricing.validator.PriceValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PriceServiceImpl")
class PriceServiceImplTest {

    @Mock private PriceRepository        priceRepository;
    @Mock private PriceListRepository    priceListRepository;
    @Mock private PriceTierRepository    priceTierRepository;
    @Mock private PriceHistoryRepository historyRepository;
    @Mock private PriceMapper            mapper;
    @Mock private PriceValidator         validator;
    @Mock private FynzaEventPublisher    eventPublisher;
    @Mock private AuditLogService        auditLogService;

    @InjectMocks
    private PriceServiceImpl service;

    private UUID actorId;
    private UUID productId;
    private UUID pricePublicId;
    private PriceList defaultPriceList;
    private Price price;
    private PriceResponse priceResponse;

    @BeforeEach
    void setUp() {
        actorId       = UUID.randomUUID();
        productId     = UUID.randomUUID();
        pricePublicId = UUID.randomUUID();

        defaultPriceList = PriceList.builder()
                .id(1L).publicId(UUID.randomUUID()).name("Default")
                .isDefault(true).isActive(true).build();

        price = Price.builder()
                .id(10L).publicId(pricePublicId).priceList(defaultPriceList)
                .productId(productId).amount(new BigDecimal("100.00"))
                .currency(SupportedCurrency.GHS).status(PriceStatus.DRAFT)
                .createdBy(actorId).isActive(true).build();

        priceResponse = PriceResponse.builder()
                .publicId(pricePublicId).productId(productId)
                .amount(new BigDecimal("100.00")).currency("GHS")
                .status("DRAFT").isActive(true).build();
    }

    @Nested
    @DisplayName("createPrice")
    class CreatePrice {

        @Test
        @DisplayName("happy path — uses default price list, saves price, records history, audits, publishes event")
        void createPrice_happyPath_savesAndReturnsResponse() {
            CreatePriceRequest request = CreatePriceRequest.builder()
                    .productId(productId).amount(new BigDecimal("100.00"))
                    .currency(SupportedCurrency.GHS).build();

            doNothing().when(validator).validateCreate(request);
            when(priceListRepository.findByIsDefaultTrue()).thenReturn(Optional.of(defaultPriceList));
            when(priceRepository.save(any(Price.class))).thenReturn(price);
            when(historyRepository.save(any())).thenReturn(null);
            when(mapper.toResponse(price)).thenReturn(priceResponse);

            PriceResponse result = service.createPrice(request, actorId);

            assertThat(result).isNotNull();
            assertThat(result.getPublicId()).isEqualTo(pricePublicId);
            verify(validator).validateCreate(request);
            verify(priceListRepository).findByIsDefaultTrue();
            verify(priceRepository).save(any(Price.class));
            verify(historyRepository).save(any());
            verify(auditLogService).log(any());
            verify(eventPublisher).publish(any());
        }

        @Test
        @DisplayName("uses provided priceListId when not null — never falls back to default")
        void createPrice_withExplicitPriceListId_loadsByPublicId() {
            UUID explicitListId = UUID.randomUUID();
            PriceList explicitList = PriceList.builder()
                    .id(2L).publicId(explicitListId).name("Wholesale")
                    .isDefault(false).isActive(true).build();

            CreatePriceRequest request = CreatePriceRequest.builder()
                    .productId(productId).amount(new BigDecimal("80.00"))
                    .currency(SupportedCurrency.GHS).priceListId(explicitListId).build();

            doNothing().when(validator).validateCreate(request);
            when(priceListRepository.findByPublicId(explicitListId)).thenReturn(Optional.of(explicitList));
            when(priceRepository.save(any(Price.class))).thenReturn(price);
            when(historyRepository.save(any())).thenReturn(null);
            when(mapper.toResponse(price)).thenReturn(priceResponse);

            service.createPrice(request, actorId);

            verify(priceListRepository).findByPublicId(explicitListId);
            verify(priceListRepository, never()).findByIsDefaultTrue();
        }

        @Test
        @DisplayName("throws PriceListNotFoundException when no default price list exists")
        void createPrice_noDefaultPriceList_throwsPriceListNotFoundException() {
            CreatePriceRequest request = CreatePriceRequest.builder()
                    .productId(productId).amount(new BigDecimal("50.00"))
                    .currency(SupportedCurrency.GHS).build();

            doNothing().when(validator).validateCreate(request);
            when(priceListRepository.findByIsDefaultTrue()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createPrice(request, actorId))
                    .isInstanceOf(PriceListNotFoundException.class);

            verify(priceRepository, never()).save(any());
        }

        @Test
        @DisplayName("propagates InvalidPriceException thrown by validator — save never called")
        void createPrice_validationFailure_propagatesException() {
            CreatePriceRequest request = CreatePriceRequest.builder()
                    .productId(productId).amount(new BigDecimal("50.00"))
                    .saleAmount(new BigDecimal("99.00")).currency(SupportedCurrency.GHS).build();

            doThrow(new InvalidPriceException("saleAmount must not exceed amount"))
                    .when(validator).validateCreate(request);

            assertThatThrownBy(() -> service.createPrice(request, actorId))
                    .isInstanceOf(InvalidPriceException.class)
                    .hasMessageContaining("saleAmount must not exceed amount");

            verify(priceRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("activatePrice")
    class ActivatePrice {

        @Test
        @DisplayName("happy path — no overlap, status transitions to ACTIVE")
        void activatePrice_noOverlap_statusBecomesActive() {
            when(priceRepository.findByPublicId(pricePublicId)).thenReturn(Optional.of(price));
            doNothing().when(validator).assertOwner(price, actorId);
            when(priceRepository.existsActiveOverlap(
                    eq(defaultPriceList.getId()), eq(productId),
                    isNull(), eq(SupportedCurrency.GHS), eq(price.getId()))).thenReturn(false);
            when(priceRepository.save(price)).thenReturn(price);
            when(historyRepository.save(any())).thenReturn(null);
            when(mapper.toResponse(price)).thenReturn(priceResponse);

            service.activatePrice(pricePublicId, actorId);

            assertThat(price.getStatus()).isEqualTo(PriceStatus.ACTIVE);
            verify(priceRepository).save(price);
            verify(auditLogService).log(any());
            verify(eventPublisher).publish(any());
        }

        @Test
        @DisplayName("throws OverlappingPriceException when an active price already exists")
        void activatePrice_overlapExists_throwsOverlappingPriceException() {
            when(priceRepository.findByPublicId(pricePublicId)).thenReturn(Optional.of(price));
            doNothing().when(validator).assertOwner(price, actorId);
            when(priceRepository.existsActiveOverlap(
                    eq(defaultPriceList.getId()), eq(productId),
                    isNull(), eq(SupportedCurrency.GHS), eq(price.getId()))).thenReturn(true);

            assertThatThrownBy(() -> service.activatePrice(pricePublicId, actorId))
                    .isInstanceOf(OverlappingPriceException.class);

            verify(priceRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws PriceNotFoundException when price does not exist")
        void activatePrice_priceNotFound_throwsPriceNotFoundException() {
            when(priceRepository.findByPublicId(pricePublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.activatePrice(pricePublicId, actorId))
                    .isInstanceOf(PriceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("disablePrice")
    class DisablePrice {

        @Test
        @DisplayName("happy path — status transitions to DISABLED, isActive set to false")
        void disablePrice_happyPath_disablesPrice() {
            price.setStatus(PriceStatus.ACTIVE);
            price.setIsActive(true);

            when(priceRepository.findByPublicId(pricePublicId)).thenReturn(Optional.of(price));
            doNothing().when(validator).assertOwner(price, actorId);
            when(priceRepository.save(price)).thenReturn(price);
            when(historyRepository.save(any())).thenReturn(null);
            when(mapper.toResponse(price)).thenReturn(priceResponse);

            service.disablePrice(pricePublicId, actorId);

            assertThat(price.getStatus()).isEqualTo(PriceStatus.DISABLED);
            assertThat(price.getIsActive()).isFalse();
            verify(auditLogService).log(any());
            verify(eventPublisher).publish(any());
        }

        @Test
        @DisplayName("throws PriceOwnershipException when actor does not own the price")
        void disablePrice_wrongOwner_throwsPriceOwnershipException() {
            UUID foreignActor = UUID.randomUUID();
            when(priceRepository.findByPublicId(pricePublicId)).thenReturn(Optional.of(price));
            doThrow(new PriceOwnershipException(pricePublicId))
                    .when(validator).assertOwner(price, foreignActor);

            assertThatThrownBy(() -> service.disablePrice(pricePublicId, foreignActor))
                    .isInstanceOf(PriceOwnershipException.class)
                    .hasMessageContaining(pricePublicId.toString());

            verify(priceRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("schedulePrice")
    class SchedulePrice {

        @Test
        @DisplayName("happy path — validFrom and validUntil set, status transitions to SCHEDULED")
        void schedulePrice_happyPath_statusBecomesScheduled() {
            Instant from  = Instant.now().plusSeconds(3600);
            Instant until = from.plusSeconds(86400);
            SchedulePriceRequest request = SchedulePriceRequest.builder()
                    .validFrom(from).validUntil(until).build();

            when(priceRepository.findByPublicId(pricePublicId)).thenReturn(Optional.of(price));
            doNothing().when(validator).assertOwner(price, actorId);
            when(priceRepository.save(price)).thenReturn(price);
            when(historyRepository.save(any())).thenReturn(null);
            when(mapper.toResponse(price)).thenReturn(priceResponse);

            service.schedulePrice(pricePublicId, request, actorId);

            assertThat(price.getStatus()).isEqualTo(PriceStatus.SCHEDULED);
            assertThat(price.getValidFrom()).isEqualTo(from);
            assertThat(price.getValidUntil()).isEqualTo(until);
            verify(eventPublisher).publish(any());
        }

        @Test
        @DisplayName("throws InvalidPriceException when validUntil equals validFrom")
        void schedulePrice_validUntilEqualsValidFrom_throwsInvalidPriceException() {
            Instant from = Instant.now().plusSeconds(3600);
            SchedulePriceRequest request = SchedulePriceRequest.builder()
                    .validFrom(from).validUntil(from).build();

            when(priceRepository.findByPublicId(pricePublicId)).thenReturn(Optional.of(price));
            doNothing().when(validator).assertOwner(price, actorId);

            assertThatThrownBy(() -> service.schedulePrice(pricePublicId, request, actorId))
                    .isInstanceOf(InvalidPriceException.class)
                    .hasMessageContaining("validUntil must be after validFrom");

            verify(priceRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updatePrice")
    class UpdatePrice {

        @Test
        @DisplayName("happy path — provided fields are patched, audit and event fired")
        void updatePrice_happyPath_patchesProvidedFields() {
            BigDecimal newAmount = new BigDecimal("120.00");
            UpdatePriceRequest request = UpdatePriceRequest.builder()
                    .amount(newAmount).reason("Seasonal adjustment").build();

            when(priceRepository.findByPublicId(pricePublicId)).thenReturn(Optional.of(price));
            doNothing().when(validator).assertOwner(price, actorId);
            doNothing().when(validator).validateUpdate(request, price);
            when(priceRepository.save(price)).thenReturn(price);
            when(historyRepository.save(any())).thenReturn(null);
            when(mapper.toResponse(price)).thenReturn(priceResponse);

            service.updatePrice(pricePublicId, request, actorId);

            assertThat(price.getAmount()).isEqualByComparingTo(newAmount);
            verify(validator).validateUpdate(request, price);
            verify(priceRepository).save(price);
            verify(auditLogService).log(any());
            verify(eventPublisher).publish(any());
        }

        @Test
        @DisplayName("null fields in request are not patched — existing values preserved")
        void updatePrice_nullFieldsNotPatched_existingValuesPreserved() {
            BigDecimal originalAmount = price.getAmount();
            UpdatePriceRequest request = UpdatePriceRequest.builder()
                    .currency(SupportedCurrency.USD).build();

            when(priceRepository.findByPublicId(pricePublicId)).thenReturn(Optional.of(price));
            doNothing().when(validator).assertOwner(price, actorId);
            doNothing().when(validator).validateUpdate(request, price);
            when(priceRepository.save(price)).thenReturn(price);
            when(historyRepository.save(any())).thenReturn(null);
            when(mapper.toResponse(price)).thenReturn(priceResponse);

            service.updatePrice(pricePublicId, request, actorId);

            assertThat(price.getAmount()).isEqualByComparingTo(originalAmount);
            assertThat(price.getCurrency()).isEqualTo(SupportedCurrency.USD);
        }
    }

    @Nested
    @DisplayName("deleteTier")
    class DeleteTier {

        @Test
        @DisplayName("happy path — tier isActive set to false, saved, audit logged")
        void deleteTier_happyPath_tierSoftDeleted() {
            UUID tierPublicId = UUID.randomUUID();
            PriceTier tier = PriceTier.builder()
                    .id(5L).publicId(tierPublicId).price(price)
                    .minQuantity(10).maxQuantity(50)
                    .unitPrice(new BigDecimal("90.00"))
                    .currency(SupportedCurrency.GHS).isActive(true).build();

            when(priceTierRepository.findByPublicId(tierPublicId)).thenReturn(Optional.of(tier));
            doNothing().when(validator).assertOwner(price, actorId);
            when(priceTierRepository.save(tier)).thenReturn(tier);

            service.deleteTier(tierPublicId, actorId);

            assertThat(tier.getIsActive()).isFalse();
            verify(priceTierRepository).save(tier);
            verify(auditLogService).log(any());
        }

        @Test
        @DisplayName("throws PriceNotFoundException when tier does not exist")
        void deleteTier_tierNotFound_throwsPriceNotFoundException() {
            UUID missingId = UUID.randomUUID();
            when(priceTierRepository.findByPublicId(missingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteTier(missingId, actorId))
                    .isInstanceOf(PriceNotFoundException.class);

            verify(priceTierRepository, never()).save(any());
        }
    }
}

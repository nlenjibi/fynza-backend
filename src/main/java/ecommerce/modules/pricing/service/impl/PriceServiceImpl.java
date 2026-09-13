package ecommerce.modules.pricing.service.impl;

import ecommerce.common.event.FynzaEventPublisher;
import ecommerce.common.event.pricing.*;
import ecommerce.modules.audit.constant.AuditAction;
import ecommerce.modules.audit.dto.AuditLogEntry;
import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.pricing.dto.request.CreatePriceRequest;
import ecommerce.modules.pricing.dto.request.CreatePriceTierRequest;
import ecommerce.modules.pricing.dto.request.SchedulePriceRequest;
import ecommerce.modules.pricing.dto.request.UpdatePriceRequest;
import ecommerce.modules.pricing.dto.response.PriceHistoryResponse;
import ecommerce.modules.pricing.dto.response.PriceResponse;
import ecommerce.modules.pricing.dto.response.PriceTierResponse;
import ecommerce.modules.pricing.entity.Price;
import ecommerce.modules.pricing.entity.PriceHistory;
import ecommerce.modules.pricing.entity.PriceList;
import ecommerce.modules.pricing.entity.PriceTier;
import ecommerce.modules.pricing.enums.PriceStatus;
import ecommerce.modules.pricing.exception.*;
import ecommerce.modules.pricing.mapper.PriceMapper;
import ecommerce.modules.pricing.repository.*;
import ecommerce.modules.pricing.service.PriceService;
import ecommerce.modules.pricing.validator.PriceValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PriceServiceImpl implements PriceService {

    private final PriceRepository        priceRepository;
    private final PriceListRepository    priceListRepository;
    private final PriceTierRepository    priceTierRepository;
    private final PriceHistoryRepository historyRepository;
    private final PriceMapper            mapper;
    private final PriceValidator         validator;
    private final FynzaEventPublisher    eventPublisher;
    private final AuditLogService        auditLogService;

    @Override
    @Transactional
    public PriceResponse createPrice(CreatePriceRequest request, UUID actorId) {
        validator.validateCreate(request);

        PriceList priceList = resolveOrDefaultPriceList(request.getPriceListId());

        Price price = Price.builder()
                .priceList(priceList)
                .productId(request.getProductId())
                .variantId(request.getVariantId())
                .amount(request.getAmount())
                .saleAmount(request.getSaleAmount())
                .currency(request.getCurrency())
                .status(PriceStatus.DRAFT)
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .createdBy(actorId)
                .isActive(true)
                .build();

        price = priceRepository.save(price);

        recordHistory(price, null, null, null, null, actorId, "Price created");
        audit(AuditAction.PRICE_CREATED, price.getPublicId(), actorId);

        eventPublisher.publish(new PriceCreatedEvent(
                price.getPublicId(), price.getProductId(), price.getVariantId(),
                price.getAmount(), price.getCurrency(), actorId));

        log.info("Price created: publicId={} product={}", price.getPublicId(), price.getProductId());
        return mapper.toResponse(price);
    }

    @Override
    @Transactional
    public PriceResponse updatePrice(UUID publicId, UpdatePriceRequest request, UUID actorId) {
        Price price = resolveOwnedPrice(publicId, actorId);
        validator.validateUpdate(request, price);

        var oldAmount   = price.getAmount();
        var oldStatus   = price.getStatus();

        if (request.getAmount()     != null) price.setAmount(request.getAmount());
        if (request.getSaleAmount() != null) price.setSaleAmount(request.getSaleAmount());
        if (request.getCurrency()   != null) price.setCurrency(request.getCurrency());
        if (request.getValidFrom()  != null) price.setValidFrom(request.getValidFrom());
        if (request.getValidUntil() != null) price.setValidUntil(request.getValidUntil());

        priceRepository.save(price);

        recordHistory(price, oldAmount, null, oldStatus.name(), null, actorId,
                request.getReason() != null ? request.getReason() : "Price updated");
        audit(AuditAction.PRICE_UPDATED, publicId, actorId);

        eventPublisher.publish(new PriceUpdatedEvent(
                price.getPublicId(), price.getProductId(), price.getVariantId(),
                oldAmount, price.getAmount(), price.getCurrency()));

        log.info("Price updated: publicId={}", publicId);
        return mapper.toResponse(price);
    }

    @Override
    @Transactional
    public PriceResponse activatePrice(UUID publicId, UUID actorId) {
        Price price = resolveOwnedPrice(publicId, actorId);

        if (priceRepository.existsActiveOverlap(
                price.getPriceList().getId(), price.getProductId(),
                price.getVariantId(), price.getCurrency(), price.getId())) {
            throw new OverlappingPriceException();
        }

        var oldStatus = price.getStatus();
        price.setStatus(PriceStatus.ACTIVE);
        priceRepository.save(price);

        recordHistory(price, price.getAmount(), price.getAmount(), oldStatus.name(), PriceStatus.ACTIVE.name(), actorId, "Price activated");
        audit(AuditAction.PRICE_ACTIVATED, publicId, actorId);

        eventPublisher.publish(new PriceActivatedEvent(
                price.getPublicId(), price.getProductId(), price.getVariantId(),
                price.getAmount(), price.getCurrency()));

        log.info("Price activated: publicId={}", publicId);
        return mapper.toResponse(price);
    }

    @Override
    @Transactional
    public PriceResponse disablePrice(UUID publicId, UUID actorId) {
        Price price = resolveOwnedPrice(publicId, actorId);

        var oldStatus = price.getStatus();
        price.setStatus(PriceStatus.DISABLED);
        price.setIsActive(false);
        priceRepository.save(price);

        recordHistory(price, price.getAmount(), price.getAmount(), oldStatus.name(), PriceStatus.DISABLED.name(), actorId, "Price disabled");
        audit(AuditAction.PRICE_DISABLED, publicId, actorId);

        eventPublisher.publish(new PriceDisabledEvent(
                price.getPublicId(), price.getProductId(), price.getVariantId()));

        log.info("Price disabled: publicId={}", publicId);
        return mapper.toResponse(price);
    }

    @Override
    @Transactional
    public PriceResponse schedulePrice(UUID publicId, SchedulePriceRequest request, UUID actorId) {
        Price price = resolveOwnedPrice(publicId, actorId);

        if (request.getValidUntil() != null && !request.getValidUntil().isAfter(request.getValidFrom())) {
            throw new InvalidPriceException("validUntil must be after validFrom");
        }

        var oldStatus = price.getStatus();
        price.setValidFrom(request.getValidFrom());
        price.setValidUntil(request.getValidUntil());
        price.setStatus(PriceStatus.SCHEDULED);
        priceRepository.save(price);

        recordHistory(price, price.getAmount(), price.getAmount(), oldStatus.name(), PriceStatus.SCHEDULED.name(), actorId, "Price scheduled");
        audit(AuditAction.PRICE_SCHEDULED, publicId, actorId);

        eventPublisher.publish(new PriceScheduledEvent(
                price.getPublicId(), price.getProductId(), price.getVariantId(),
                request.getValidFrom(), request.getValidUntil()));

        log.info("Price scheduled: publicId={} validFrom={}", publicId, request.getValidFrom());
        return mapper.toResponse(price);
    }

    @Override
    public Page<PriceResponse> getSellerPrices(UUID productId, UUID actorId, Pageable pageable) {
        return priceRepository.findByProductIdAndCreatedBy(productId, actorId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    public List<PriceHistoryResponse> getPriceHistory(UUID publicId, UUID actorId) {
        Price price = resolveOwnedPrice(publicId, actorId);
        return historyRepository.findByPrice_IdOrderByCreatedAtDesc(price.getId())
                .stream().map(mapper::toHistoryResponse).toList();
    }

    // ── Price Tier operations ─────────────────────────────────────────────────

    @Override
    @Transactional
    public PriceTierResponse createTier(CreatePriceTierRequest request, UUID actorId) {
        Price price = resolveOwnedPrice(request.getPriceId(), actorId);

        if (request.getMaxQuantity() != null && request.getMaxQuantity() < request.getMinQuantity()) {
            throw new InvalidPriceException("maxQuantity must be >= minQuantity");
        }

        PriceTier tier = PriceTier.builder()
                .price(price)
                .minQuantity(request.getMinQuantity())
                .maxQuantity(request.getMaxQuantity())
                .unitPrice(request.getUnitPrice())
                .currency(request.getCurrency())
                .isActive(true)
                .build();

        priceTierRepository.save(tier);
        audit(AuditAction.PRICE_TIER_CREATED, price.getPublicId(), actorId);

        log.info("Price tier created: priceId={} minQty={}", price.getPublicId(), request.getMinQuantity());
        return mapper.toTierResponse(tier);
    }

    @Override
    @Transactional
    public PriceTierResponse updateTier(UUID tierPublicId, CreatePriceTierRequest request, UUID actorId) {
        PriceTier tier = priceTierRepository.findByPublicId(tierPublicId)
                .orElseThrow(() -> new PriceNotFoundException(tierPublicId));

        validator.assertOwner(tier.getPrice(), actorId);

        if (request.getMaxQuantity() != null && request.getMaxQuantity() < request.getMinQuantity()) {
            throw new InvalidPriceException("maxQuantity must be >= minQuantity");
        }

        tier.setMinQuantity(request.getMinQuantity());
        tier.setMaxQuantity(request.getMaxQuantity());
        tier.setUnitPrice(request.getUnitPrice());
        tier.setCurrency(request.getCurrency());
        priceTierRepository.save(tier);

        audit(AuditAction.PRICE_TIER_UPDATED, tier.getPrice().getPublicId(), actorId);
        return mapper.toTierResponse(tier);
    }

    @Override
    @Transactional
    public void deleteTier(UUID tierPublicId, UUID actorId) {
        PriceTier tier = priceTierRepository.findByPublicId(tierPublicId)
                .orElseThrow(() -> new PriceNotFoundException(tierPublicId));

        validator.assertOwner(tier.getPrice(), actorId);
        tier.setIsActive(false);
        priceTierRepository.save(tier);

        audit(AuditAction.PRICE_TIER_DELETED, tier.getPrice().getPublicId(), actorId);
        log.info("Price tier soft-deleted: publicId={}", tierPublicId);
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private Price resolveOwnedPrice(UUID publicId, UUID actorId) {
        Price price = priceRepository.findByPublicId(publicId)
                .orElseThrow(() -> new PriceNotFoundException(publicId));
        validator.assertOwner(price, actorId);
        return price;
    }

    private PriceList resolveOrDefaultPriceList(UUID priceListId) {
        if (priceListId != null) {
            return priceListRepository.findByPublicId(priceListId)
                    .orElseThrow(() -> new PriceListNotFoundException(priceListId));
        }
        return priceListRepository.findByIsDefaultTrue()
                .orElseThrow(() -> new PriceListNotFoundException("default"));
    }

    private void recordHistory(Price price, java.math.BigDecimal oldAmount, java.math.BigDecimal newAmount,
                                String oldStatus, String newStatus, UUID changedBy, String reason) {
        PriceHistory history = PriceHistory.builder()
                .price(price)
                .oldAmount(oldAmount)
                .newAmount(newAmount != null ? newAmount : price.getAmount())
                .oldCurrency(oldAmount != null && price.getCurrency() != null ? price.getCurrency().name() : null)
                .newCurrency(price.getCurrency().name())
                .oldStatus(oldStatus)
                .newStatus(newStatus != null ? newStatus : price.getStatus().name())
                .changedBy(changedBy)
                .reason(reason)
                .build();
        historyRepository.save(history);
    }

    private void audit(String action, UUID entityPublicId, UUID actorId) {
        auditLogService.log(AuditLogEntry.builder()
                .action(action)
                .entityType("PRICE")
                .entityPublicId(entityPublicId)
                .actorPublicId(actorId)
                .status(AuditLogEntry.STATUS_SUCCESS)
                .build());
    }
}

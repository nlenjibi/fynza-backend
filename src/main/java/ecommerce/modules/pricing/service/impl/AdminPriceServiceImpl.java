package ecommerce.modules.pricing.service.impl;

import ecommerce.common.event.FynzaEventPublisher;
import ecommerce.common.event.pricing.PriceOverrideCreatedEvent;
import ecommerce.common.event.pricing.PriceUpdatedEvent;
import ecommerce.modules.audit.constant.AuditAction;
import ecommerce.modules.audit.dto.AuditLogEntry;
import ecommerce.modules.audit.service.AuditLogService;
import ecommerce.modules.pricing.dto.request.PriceOverrideRequest;
import ecommerce.modules.pricing.dto.request.UpdatePriceRequest;
import ecommerce.modules.pricing.dto.response.PriceHistoryResponse;
import ecommerce.modules.pricing.dto.response.PriceResponse;
import ecommerce.modules.pricing.entity.Price;
import ecommerce.modules.pricing.entity.PriceHistory;
import ecommerce.modules.pricing.entity.PriceOverride;
import ecommerce.modules.pricing.exception.PriceNotFoundException;
import ecommerce.modules.pricing.exception.InvalidPriceException;
import ecommerce.modules.pricing.mapper.PriceMapper;
import ecommerce.modules.pricing.repository.PriceHistoryRepository;
import ecommerce.modules.pricing.repository.PriceOverrideRepository;
import ecommerce.modules.pricing.repository.PriceRepository;
import ecommerce.modules.pricing.service.AdminPriceService;
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
public class AdminPriceServiceImpl implements AdminPriceService {

    private final PriceRepository        priceRepository;
    private final PriceHistoryRepository historyRepository;
    private final PriceOverrideRepository overrideRepository;
    private final PriceMapper            mapper;
    private final PriceValidator         validator;
    private final FynzaEventPublisher    eventPublisher;
    private final AuditLogService        auditLogService;

    @Override
    public Page<PriceResponse> getAllPrices(Pageable pageable) {
        return priceRepository.findAll(pageable).map(mapper::toResponse);
    }

    @Override
    public PriceResponse getPriceById(UUID publicId) {
        return mapper.toResponse(resolve(publicId));
    }

    @Override
    @Transactional
    public PriceResponse updatePrice(UUID publicId, UpdatePriceRequest request, UUID adminId) {
        Price price = resolve(publicId);
        validator.validateUpdate(request, price);

        var oldAmount = price.getAmount();
        var oldStatus = price.getStatus();

        if (request.getAmount()     != null) price.setAmount(request.getAmount());
        if (request.getSaleAmount() != null) price.setSaleAmount(request.getSaleAmount());
        if (request.getCurrency()   != null) price.setCurrency(request.getCurrency());
        if (request.getValidFrom()  != null) price.setValidFrom(request.getValidFrom());
        if (request.getValidUntil() != null) price.setValidUntil(request.getValidUntil());

        priceRepository.save(price);

        saveHistory(price, oldAmount, oldStatus.name(), adminId,
                request.getReason() != null ? request.getReason() : "Admin price update");
        audit(AuditAction.PRICE_UPDATED, publicId, adminId);

        eventPublisher.publish(new PriceUpdatedEvent(
                price.getPublicId(), price.getProductId(), price.getVariantId(),
                oldAmount, price.getAmount(), price.getCurrency()));

        log.info("Admin updated price: publicId={} admin={}", publicId, adminId);
        return mapper.toResponse(price);
    }

    @Override
    @Transactional
    public PriceResponse overridePrice(UUID publicId, PriceOverrideRequest request, UUID adminId) {
        Price price = resolve(publicId);

        if (request.getNewAmount().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new InvalidPriceException("newAmount must be >= 0");
        }

        var oldAmount = price.getAmount();

        PriceOverride override = PriceOverride.builder()
                .price(price)
                .reason(request.getReason())
                .oldAmount(oldAmount)
                .newAmount(request.getNewAmount())
                .approvedBy(adminId)
                .build();
        overrideRepository.save(override);

        price.setAmount(request.getNewAmount());
        priceRepository.save(price);

        saveHistory(price, oldAmount, price.getStatus().name(), adminId,
                "Admin override: " + request.getReason());
        audit(AuditAction.PRICE_OVERRIDDEN, publicId, adminId);

        eventPublisher.publish(new PriceOverrideCreatedEvent(
                price.getPublicId(), price.getProductId(),
                oldAmount, request.getNewAmount(), adminId, request.getReason()));

        log.info("Price overridden: publicId={} admin={} old={} new={}",
                publicId, adminId, oldAmount, request.getNewAmount());
        return mapper.toResponse(price);
    }

    @Override
    public List<PriceResponse> getPricesByProductId(UUID productId) {
        return priceRepository.findAllByProductId(productId)
                .stream().map(mapper::toResponse).toList();
    }

    @Override
    public List<PriceHistoryResponse> getPriceHistory(UUID publicId) {
        Price price = resolve(publicId);
        return historyRepository.findByPrice_IdOrderByCreatedAtDesc(price.getId())
                .stream().map(mapper::toHistoryResponse).toList();
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private Price resolve(UUID publicId) {
        return priceRepository.findByPublicId(publicId)
                .orElseThrow(() -> new PriceNotFoundException(publicId));
    }

    private void saveHistory(Price price, java.math.BigDecimal oldAmount, String oldStatus,
                              UUID changedBy, String reason) {
        historyRepository.save(PriceHistory.builder()
                .price(price)
                .oldAmount(oldAmount)
                .newAmount(price.getAmount())
                .oldCurrency(price.getCurrency().name())
                .newCurrency(price.getCurrency().name())
                .oldStatus(oldStatus)
                .newStatus(price.getStatus().name())
                .changedBy(changedBy)
                .reason(reason)
                .build());
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

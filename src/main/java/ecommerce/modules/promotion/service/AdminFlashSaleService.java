package ecommerce.modules.promotion.service;

import ecommerce.common.exception.BadRequestException;
import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.promotion.entity.AdminFlashSale;
import ecommerce.modules.promotion.entity.AdminFlashSale.Status;
import ecommerce.modules.promotion.repository.AdminFlashSaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminFlashSaleService {

    private final AdminFlashSaleRepository adminFlashSaleRepository;

    @Transactional
    public AdminFlashSale createFlashSale(UUID createdBy, String name, String description,
                                         Integer discountPercent, BigDecimal minPurchaseAmount,
                                         BigDecimal maxDiscountAmount, Integer maxProductsPerSeller,
                                         Integer maxTotalProducts, String category,
                                         LocalDateTime startDatetime, LocalDateTime endDatetime) {
        AdminFlashSale flashSale = AdminFlashSale.builder()
                .name(name)
                .description(description)
                .discountPercent(discountPercent)
                .minPurchaseAmount(minPurchaseAmount)
                .maxDiscountAmount(maxDiscountAmount)
                .maxProductsPerSeller(maxProductsPerSeller)
                .maxTotalProducts(maxTotalProducts)
                .category(category)
                .startDatetime(startDatetime)
                .endDatetime(endDatetime)
                .status(determineStatus(startDatetime, endDatetime))
                .createdBy(createdBy)
                .build();

        log.info("Creating flash sale: {}", name);
        return adminFlashSaleRepository.save(flashSale);
    }

    @Transactional
    public AdminFlashSale updateFlashSale(UUID id, String name, String description,
                                         Integer discountPercent, BigDecimal minPurchaseAmount,
                                         BigDecimal maxDiscountAmount, LocalDateTime startDatetime,
                                         LocalDateTime endDatetime) {
        AdminFlashSale flashSale = adminFlashSaleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flash sale not found"));

        if (name != null) flashSale.setName(name);
        if (description != null) flashSale.setDescription(description);
        if (discountPercent != null) flashSale.setDiscountPercent(discountPercent);
        if (minPurchaseAmount != null) flashSale.setMinPurchaseAmount(minPurchaseAmount);
        if (maxDiscountAmount != null) flashSale.setMaxDiscountAmount(maxDiscountAmount);
        if (startDatetime != null) flashSale.setStartDatetime(startDatetime);
        if (endDatetime != null) flashSale.setEndDatetime(endDatetime);

        flashSale.setStatus(determineStatus(flashSale.getStartDatetime(), flashSale.getEndDatetime()));

        log.info("Updating flash sale: {}", id);
        return adminFlashSaleRepository.save(flashSale);
    }

    @Transactional
    public void deleteFlashSale(UUID id) {
        AdminFlashSale flashSale = adminFlashSaleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flash sale not found"));
        flashSale.setIsActive(false);
        adminFlashSaleRepository.save(flashSale);
        log.info("Deleted flash sale: {}", id);
    }

    @Transactional(readOnly = true)
    public AdminFlashSale getFlashSale(UUID id) {
        return adminFlashSaleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flash sale not found"));
    }

    @Transactional(readOnly = true)
    public Page<AdminFlashSale> getAllFlashSales(Pageable pageable) {
        return adminFlashSaleRepository.findAllByOrderByStartDatetimeDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<AdminFlashSale> getFlashSalesByStatus(Status status, Pageable pageable) {
        return adminFlashSaleRepository.findByStatusOrderByStartDatetimeDesc(status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AdminFlashSale> searchFlashSales(String query, Pageable pageable) {
        return adminFlashSaleRepository.search(query, pageable);
    }

    @Transactional(readOnly = true)
    public long countActive() {
        return adminFlashSaleRepository.countActive();
    }

    @Transactional(readOnly = true)
    public long getSlotsRemaining(UUID id) {
        AdminFlashSale flashSale = getFlashSale(id);
        return flashSale.getSlotsRemaining();
    }

    private Status determineStatus(LocalDateTime startDatetime, LocalDateTime endDatetime) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startDatetime)) return Status.SCHEDULED;
        if (now.isAfter(endDatetime)) return Status.EXPIRED;
        return Status.ACTIVE;
    }
}

package ecommerce.modules.promotion.service;

import ecommerce.common.exception.BadRequestException;
import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.promotion.entity.AdminFlashSale;
import ecommerce.modules.promotion.entity.SellerFlashSaleApplication;
import ecommerce.modules.promotion.entity.SellerFlashSaleApplication.Status;
import ecommerce.modules.promotion.repository.AdminFlashSaleRepository;
import ecommerce.modules.promotion.repository.SellerFlashSaleApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerFlashSaleApplicationService {

    private final SellerFlashSaleApplicationRepository applicationRepository;
    private final AdminFlashSaleRepository adminFlashSaleRepository;

    @Transactional
    public SellerFlashSaleApplication applyToFlashSale(UUID flashSaleId, UUID sellerId) {
        AdminFlashSale flashSale = adminFlashSaleRepository.findById(flashSaleId)
                .orElseThrow(() -> new ResourceNotFoundException("Flash sale not found"));

        if (flashSale.getStatus() != AdminFlashSale.Status.ACTIVE && 
            flashSale.getStatus() != AdminFlashSale.Status.SCHEDULED) {
            throw new BadRequestException("Flash sale is not accepting applications");
        }

        if (applicationRepository.existsByFlashSaleIdAndSellerId(flashSaleId, sellerId)) {
            throw new BadRequestException("Already applied to this flash sale");
        }

        SellerFlashSaleApplication application = SellerFlashSaleApplication.builder()
                .flashSaleId(flashSaleId)
                .sellerId(sellerId)
                .status(Status.PENDING)
                .build();

        log.info("Seller {} applied to flash sale {}", sellerId, flashSaleId);
        return applicationRepository.save(application);
    }

    @Transactional
    public SellerFlashSaleApplication approveApplication(UUID applicationId, UUID reviewedBy, String note) {
        SellerFlashSaleApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        application.setStatus(Status.APPROVED);
        application.setReviewedBy(reviewedBy);
        application.setReviewedAt(LocalDateTime.now());
        application.setReviewNote(note);

        adminFlashSaleRepository.incrementProductCount(application.getFlashSaleId(), 1);

        log.info("Application {} approved", applicationId);
        return applicationRepository.save(application);
    }

    @Transactional
    public SellerFlashSaleApplication rejectApplication(UUID applicationId, UUID reviewedBy, String note) {
        SellerFlashSaleApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        application.setStatus(Status.REJECTED);
        application.setReviewedBy(reviewedBy);
        application.setReviewedAt(LocalDateTime.now());
        application.setReviewNote(note);

        log.info("Application {} rejected", applicationId);
        return applicationRepository.save(application);
    }

    @Transactional(readOnly = true)
    public Page<SellerFlashSaleApplication> getSellerApplications(UUID sellerId, Pageable pageable) {
        return applicationRepository.findBySellerId(sellerId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<SellerFlashSaleApplication> getPendingApplications(Pageable pageable) {
        return applicationRepository.findPendingApplications(pageable);
    }

    @Transactional(readOnly = true)
    public Page<SellerFlashSaleApplication> getFlashSaleApplications(UUID flashSaleId, Pageable pageable) {
        return applicationRepository.findByFlashSaleId(flashSaleId, pageable);
    }

    @Transactional(readOnly = true)
    public SellerFlashSaleApplication getApplication(UUID flashSaleId, UUID sellerId) {
        return applicationRepository.findByFlashSaleIdAndSellerId(flashSaleId, sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
    }
}

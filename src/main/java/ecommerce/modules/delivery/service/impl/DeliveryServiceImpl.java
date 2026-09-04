package ecommerce.modules.delivery.service.impl;

import ecommerce.common.exception.BadRequestException;
import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.delivery.dto.DeliveryFeeRequest;
import ecommerce.modules.delivery.dto.DeliveryFeeResponse;
import ecommerce.modules.delivery.dto.DeliveryRegionRequest;
import ecommerce.modules.delivery.dto.DeliveryRegionResponse;
import ecommerce.modules.delivery.entity.DeliveryFee;
import ecommerce.modules.delivery.entity.DeliveryRegion;
import ecommerce.modules.delivery.repository.DeliveryFeeRepository;
import ecommerce.modules.delivery.repository.DeliveryRegionRepository;
import ecommerce.modules.delivery.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRegionRepository regionRepository;
    private final DeliveryFeeRepository feeRepository;

    // ==================== Region Operations ====================

    @Override
    @Transactional
    public DeliveryRegionResponse createRegion(DeliveryRegionRequest request) {
        log.info("Creating new delivery region: {}", request.getName());

        if (regionRepository.existsByCode(request.getCode())) {
            throw new BadRequestException("Region code already exists: " + request.getCode());
        }

        if (regionRepository.existsByName(request.getName())) {
            throw new BadRequestException("Region name already exists: " + request.getName());
        }

        DeliveryRegion region = DeliveryRegion.builder()
                .name(request.getName())
                .code(request.getCode().toUpperCase())
                .country(request.getCountry() != null ? request.getCountry() : "Ghana")
                .isActive(true)
                .build();

        DeliveryRegion saved = regionRepository.save(region);
        log.info("Delivery region created with ID: {}", saved.getId());

        return DeliveryRegionResponse.from(saved);
    }

    @Override
    @Transactional
    public DeliveryRegionResponse updateRegion(UUID id, DeliveryRegionRequest request) {
        log.info("Updating delivery region: {}", id);

        DeliveryRegion region = regionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Region not found with ID: " + id));

        if (!region.getCode().equalsIgnoreCase(request.getCode()) 
                && regionRepository.existsByCode(request.getCode())) {
            throw new BadRequestException("Region code already exists: " + request.getCode());
        }

        region.setName(request.getName());
        region.setCode(request.getCode().toUpperCase());
        if (request.getCountry() != null) {
            region.setCountry(request.getCountry());
        }

        DeliveryRegion saved = regionRepository.save(region);
        log.info("Delivery region updated: {}", id);

        return DeliveryRegionResponse.from(saved);
    }

    @Override
    @Transactional
    public void deleteRegion(UUID id) {
        log.info("Deleting delivery region: {}", id);

        DeliveryRegion region = regionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Region not found with ID: " + id));

        region.setIsActive(false);
        regionRepository.save(region);
        log.info("Delivery region soft deleted: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DeliveryRegionResponse> getAllRegions(Pageable pageable) {
        return regionRepository.findAll(pageable)
                .map(DeliveryRegionResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryRegionResponse getRegionById(UUID id) {
        DeliveryRegion region = regionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Region not found with ID: " + id));
        return DeliveryRegionResponse.from(region);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryRegionResponse> getActiveRegions() {
        return regionRepository.findAllActive().stream()
                .map(DeliveryRegionResponse::from)
                .collect(Collectors.toList());
    }

    // ==================== Fee Operations ====================

    @Override
    @Transactional
    public DeliveryFeeResponse createDeliveryFee(DeliveryFeeRequest request) {
        log.info("Creating delivery fee for town: {}", request.getTownName());

        DeliveryRegion region = null;
        if (request.getRegionId() != null) {
            region = regionRepository.findById(request.getRegionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Region not found with ID: " + request.getRegionId()));
        }

        DeliveryFee fee = DeliveryFee.builder()
                .townName(request.getTownName())
                .deliveryMethod(request.getDeliveryMethod())
                .baseFee(request.getBaseFee())
                .perKmFee(request.getPerKmFee())
                .estimatedDays(request.getEstimatedDays())
                .region(region)
                .isActive(true)
                .build();

        DeliveryFee saved = feeRepository.save(fee);
        log.info("Delivery fee created with ID: {}", saved.getId());

        return DeliveryFeeResponse.from(saved);
    }

    @Override
    @Transactional
    public DeliveryFeeResponse updateDeliveryFee(UUID id, DeliveryFeeRequest request) {
        log.info("Updating delivery fee: {}", id);

        DeliveryFee fee = feeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery fee not found with ID: " + id));

        DeliveryRegion region = null;
        if (request.getRegionId() != null) {
            region = regionRepository.findById(request.getRegionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Region not found with ID: " + request.getRegionId()));
        }

        fee.setTownName(request.getTownName());
        fee.setDeliveryMethod(request.getDeliveryMethod());
        fee.setBaseFee(request.getBaseFee());
        fee.setPerKmFee(request.getPerKmFee());
        fee.setEstimatedDays(request.getEstimatedDays());
        fee.setRegion(region);

        DeliveryFee saved = feeRepository.save(fee);
        log.info("Delivery fee updated: {}", id);

        return DeliveryFeeResponse.from(saved);
    }

    @Override
    @Transactional
    public void deleteDeliveryFee(UUID id) {
        log.info("Deleting delivery fee: {}", id);

        DeliveryFee fee = feeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery fee not found with ID: " + id));

        fee.setIsActive(false);
        feeRepository.save(fee);
        log.info("Delivery fee soft deleted: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DeliveryFeeResponse> getAllDeliveryFees(Pageable pageable) {
        return feeRepository.findAll(pageable)
                .map(DeliveryFeeResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryFeeResponse getDeliveryFeeById(UUID id) {
        DeliveryFee fee = feeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery fee not found with ID: " + id));
        return DeliveryFeeResponse.from(fee);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryFeeResponse> getActiveDeliveryFees() {
        return feeRepository.findAllActive().stream()
                .map(DeliveryFeeResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryFeeResponse> getDeliveryFeesByRegion(UUID regionId) {
        return feeRepository.findByRegionId(regionId).stream()
                .map(DeliveryFeeResponse::from)
                .collect(Collectors.toList());
    }
}

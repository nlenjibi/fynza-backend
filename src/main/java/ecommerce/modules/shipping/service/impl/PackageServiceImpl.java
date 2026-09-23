package ecommerce.modules.shipping.service.impl;

import ecommerce.modules.shipping.dto.request.PackageRequest;
import ecommerce.modules.shipping.dto.response.PackageResponse;
import ecommerce.modules.shipping.entity.Shipment;
import ecommerce.modules.shipping.entity.ShipmentPackage;
import ecommerce.modules.shipping.enums.PackageType;
import ecommerce.modules.shipping.exception.ShipmentNotFoundException;
import ecommerce.modules.shipping.repository.PackageRepository;
import ecommerce.modules.shipping.repository.ShipmentRepository;
import ecommerce.modules.shipping.service.PackageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PackageServiceImpl implements PackageService {

    private final PackageRepository packageRepository;
    private final ShipmentRepository shipmentRepository;

    @Override
    @Transactional
    public PackageResponse addPackage(UUID shipmentPublicId, PackageRequest request) {
        Shipment shipment = shipmentRepository.findByPublicId(shipmentPublicId)
                .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found: " + shipmentPublicId));

        int sequence = packageRepository.countByShipmentId(shipment.getPublicId()) + 1;
        String packageNumber = "PKG-" + shipment.getShipmentNumber() + "-" + sequence;

        ShipmentPackage pkg = ShipmentPackage.builder()
                .shipmentId(shipment.getPublicId())
                .packageNumber(packageNumber)
                .weightKg(request.getWeightKg())
                .lengthCm(request.getLengthCm())
                .widthCm(request.getWidthCm())
                .heightCm(request.getHeightCm())
                .packageType(request.getPackageType() != null ? request.getPackageType() : PackageType.BOX)
                .labelReference(request.getLabelReference())
                .build();

        pkg = packageRepository.save(pkg);
        log.info("Added package={} to shipment={}", pkg.getPackageNumber(), shipment.getShipmentNumber());
        return PackageResponse.from(pkg);
    }

    @Override
    public List<PackageResponse> getPackagesForShipment(UUID shipmentPublicId) {
        Shipment shipment = shipmentRepository.findByPublicId(shipmentPublicId)
                .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found: " + shipmentPublicId));
        return packageRepository.findByShipmentIdAndIsActiveTrue(shipment.getPublicId())
                .stream().map(PackageResponse::from).toList();
    }

    @Override
    @Transactional
    public void removePackage(UUID packagePublicId) {
        ShipmentPackage pkg = packageRepository.findByPublicId(packagePublicId)
                .orElseThrow(() -> new ShipmentNotFoundException("Package not found: " + packagePublicId));
        pkg.setIsActive(false);
        packageRepository.save(pkg);
        log.info("Removed package={}", pkg.getPackageNumber());
    }
}

package ecommerce.modules.shipping.service;

import ecommerce.modules.shipping.dto.request.PackageRequest;
import ecommerce.modules.shipping.dto.response.PackageResponse;

import java.util.List;
import java.util.UUID;

public interface PackageService {

    PackageResponse addPackage(UUID shipmentPublicId, PackageRequest request);

    List<PackageResponse> getPackagesForShipment(UUID shipmentPublicId);

    void removePackage(UUID packagePublicId);
}

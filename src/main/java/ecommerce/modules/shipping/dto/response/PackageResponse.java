package ecommerce.modules.shipping.dto.response;

import ecommerce.modules.shipping.entity.ShipmentPackage;
import ecommerce.modules.shipping.enums.PackageType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class PackageResponse {
    UUID publicId;
    UUID shipmentId;
    String packageNumber;
    BigDecimal weightKg;
    BigDecimal lengthCm;
    BigDecimal widthCm;
    BigDecimal heightCm;
    PackageType packageType;
    String labelReference;
    Instant createdAt;

    public static PackageResponse from(ShipmentPackage pkg) {
        return PackageResponse.builder()
                .publicId(pkg.getPublicId())
                .shipmentId(pkg.getShipmentId())
                .packageNumber(pkg.getPackageNumber())
                .weightKg(pkg.getWeightKg())
                .lengthCm(pkg.getLengthCm())
                .widthCm(pkg.getWidthCm())
                .heightCm(pkg.getHeightCm())
                .packageType(pkg.getPackageType())
                .labelReference(pkg.getLabelReference())
                .createdAt(pkg.getCreatedAt())
                .build();
    }
}

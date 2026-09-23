package ecommerce.modules.shipping.provider.impl;

import ecommerce.modules.shipping.provider.ShipmentProviderRequest;
import ecommerce.modules.shipping.provider.ShipmentProviderResult;
import ecommerce.modules.shipping.provider.ShippingProvider;
import ecommerce.modules.shipping.provider.ShippingProviderType;
import ecommerce.modules.shipping.provider.dto.LabelResult;
import ecommerce.modules.shipping.provider.dto.TrackingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "fynza.shipping.provider", havingValue = "mock", matchIfMissing = true)
@Slf4j
public class MockShippingProvider implements ShippingProvider {

    @Override
    public ShippingProviderType providerType() {
        return ShippingProviderType.MOCK;
    }

    @Override
    public ShipmentProviderResult createShipment(ShipmentProviderRequest request) {
        log.info("MockShippingProvider: creating shipment for recipient={}", request.getRecipientName());
        String trackingNumber = "MOCK-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        return ShipmentProviderResult.builder()
                .trackingNumber(trackingNumber)
                .labelUrl("https://mock.shipping.example.com/labels/" + trackingNumber + ".pdf")
                .carrierLabelId(UUID.randomUUID().toString())
                .cost(request.getWeightKg() != null
                        ? request.getWeightKg().multiply(BigDecimal.valueOf(5))
                        : BigDecimal.valueOf(15))
                .estimatedDeliveryDate(LocalDate.now().plusDays(3))
                .build();
    }

    @Override
    public boolean cancelShipment(String trackingNumber) {
        log.info("MockShippingProvider: cancelling shipment trackingNumber={}", trackingNumber);
        return true;
    }

    @Override
    public LabelResult generateLabel(UUID shipmentPublicId, ShipmentProviderRequest request) {
        log.info("MockShippingProvider: generating label for shipment={}", shipmentPublicId);
        String carrierLabelId = UUID.randomUUID().toString();
        return LabelResult.builder()
                .labelUrl("https://mock.shipping.example.com/labels/" + carrierLabelId + ".pdf")
                .carrierLabelId(carrierLabelId)
                .format("PDF")
                .expiresAt(Instant.now().plusSeconds(86400 * 30))
                .build();
    }

    @Override
    public TrackingResult getTracking(String trackingNumber) {
        log.info("MockShippingProvider: fetching tracking for trackingNumber={}", trackingNumber);
        return TrackingResult.builder()
                .trackingNumber(trackingNumber)
                .status("IN_TRANSIT")
                .location("Accra Hub")
                .description("Shipment is in transit to the destination city")
                .eventTime(Instant.now())
                .build();
    }
}

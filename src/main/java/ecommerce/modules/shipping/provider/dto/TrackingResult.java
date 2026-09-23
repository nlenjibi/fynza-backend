package ecommerce.modules.shipping.provider.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class TrackingResult {
    private String trackingNumber;
    private String status;
    private String location;
    private String description;
    private Instant eventTime;
}

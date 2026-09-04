package ecommerce.modules.seller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerNotificationSettingsResponse {
    private UUID id;
    private Boolean newOrders;
    private Boolean orderUpdates;
    private Boolean customerMessages;
    private Boolean stockAlerts;
    private Boolean paymentUpdates;
    private Boolean refundRequests;
    private Boolean promotionalEmails;
    private LocalDateTime updatedAt;
}

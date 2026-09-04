package ecommerce.modules.seller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerNotificationSettingsRequest {
    private Boolean newOrders;
    private Boolean orderUpdates;
    private Boolean customerMessages;
    private Boolean stockAlerts;
    private Boolean paymentUpdates;
    private Boolean refundRequests;
    private Boolean promotionalEmails;
}

package ecommerce.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerNotificationSettingsInput {
    private Boolean newOrders;
    private Boolean orderUpdates;
    private Boolean customerMessages;
    private Boolean stockAlerts;
    private Boolean paymentUpdates;
    private Boolean refundRequests;
    private Boolean promotionalEmails;
}

package ecommerce.modules.settings.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingsRequest {
    private Boolean orderUpdates;
    private Boolean paymentConfirmation;
    private Boolean shippingUpdates;
    private Boolean promotionalEmails;
    private Boolean newProductAlerts;
    private Boolean priceDropAlerts;
    private Boolean wishlistUpdates;
    private Boolean reviewRequests;
    private Boolean newsletter;
    private Boolean promotionalSms;
    private Boolean browserNotifications;
    private Boolean appNotifications;
}

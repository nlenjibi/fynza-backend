package ecommerce.modules.notification.dto;

import ecommerce.modules.notification.entity.NotificationQuietHours;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QuietHoursResponse {

    String startTime;
    String endTime;
    String timezone;

    public static QuietHoursResponse from(NotificationQuietHours qh) {
        return QuietHoursResponse.builder()
                .startTime(qh.getStartTime().toString())
                .endTime(qh.getEndTime().toString())
                .timezone(qh.getTimezone())
                .build();
    }
}

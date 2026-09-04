package ecommerce.modules.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderTimelineResponse {
    private UUID activityId;
    private String activityType;
    private String status;
    private String description;
    private String oldValue;
    private String newValue;
    private LocalDateTime timestamp;
    private String icon;
    private String color;
}

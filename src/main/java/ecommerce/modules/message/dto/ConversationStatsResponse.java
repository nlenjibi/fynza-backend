package ecommerce.modules.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationStatsResponse {
    private long unreadCount;
    private long openCount;
    private long pendingCount;
    private long resolvedCount;
}

package ecommerce.modules.faq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FAQStatsResponse {
    private long totalFAQs;
    private long activeFAQs;
    private long draftFAQs;
    private long totalViews;
}

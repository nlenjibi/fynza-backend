package ecommerce.graphql.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class FAQStats {
    private Long totalFAQs;
    private Long activeFAQs;
    private Long draftFAQs;
    private Long totalViews;
}

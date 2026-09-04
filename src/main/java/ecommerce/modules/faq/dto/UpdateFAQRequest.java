package ecommerce.modules.faq.dto;

import ecommerce.common.enums.FAQCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFAQRequest {
    private String question;
    private String answer;
    private FAQCategory category;
    private Integer displayOrder;
}

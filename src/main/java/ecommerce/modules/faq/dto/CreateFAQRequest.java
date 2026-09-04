package ecommerce.modules.faq.dto;

import ecommerce.common.enums.FAQCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFAQRequest {
    
    @NotBlank(message = "Question is required")
    private String question;
    
    @NotBlank(message = "Answer is required")
    private String answer;
    
    @NotNull(message = "Category is required")
    private FAQCategory category;
    
    private Integer displayOrder;
}

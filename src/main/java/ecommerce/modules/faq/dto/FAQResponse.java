package ecommerce.modules.faq.dto;

import ecommerce.common.enums.FAQCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FAQResponse {
    private UUID id;
    private String question;
    private String answer;
    private FAQCategory category;
    private Boolean isActive;
    private Integer viewCount;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

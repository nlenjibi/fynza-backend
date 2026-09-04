package ecommerce.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import ecommerce.common.enums.FAQCategory;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FAQCreateInput {
    private String question;
    private String answer;
    private FAQCategory category;
    private Integer displayOrder;
}

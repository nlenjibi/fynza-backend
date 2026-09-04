package ecommerce.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import ecommerce.common.enums.FAQCategory;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UpdateFAQInput {
    private String question;
    private String answer;
    private FAQCategory category;
    private Integer displayOrder;
}

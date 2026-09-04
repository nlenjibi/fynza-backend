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
public class FAQFilterInput {
    private FAQCategory category;
    private Boolean isActive;
    private String search;
}

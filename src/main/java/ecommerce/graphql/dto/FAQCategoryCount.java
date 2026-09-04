package ecommerce.graphql.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import ecommerce.common.enums.FAQCategory;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class FAQCategoryCount {
    private FAQCategory category;
    private Integer count;
}

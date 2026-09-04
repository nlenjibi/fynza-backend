package ecommerce.graphql.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import ecommerce.common.response.PaginatedResponse;
import ecommerce.modules.faq.dto.FAQResponse;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class FAQConnection {
    private List<FAQResponse> content;
    private PaginatedResponse<FAQResponse> pageInfo;
}

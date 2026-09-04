package ecommerce.graphql.dto;

import ecommerce.common.response.PaginatedResponse;
import ecommerce.modules.promotion.dto.SellerPromotionDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerPromotionPage {
    private List<SellerPromotionDto> content;
    private PaginatedResponse<SellerPromotionDto> pageInfo;
}

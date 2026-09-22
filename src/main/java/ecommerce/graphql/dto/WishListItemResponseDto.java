package ecommerce.graphql.dto;

import ecommerce.common.response.PaginatedResponse;
import ecommerce.modules.wishlist.dto.response.WishlistResponse;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class WishListItemResponseDto {
    private List<WishlistResponse> content;
    private PaginatedResponse<WishlistResponse> pageInfo;
}

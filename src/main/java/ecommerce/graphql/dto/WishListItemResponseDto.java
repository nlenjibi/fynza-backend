package ecommerce.graphql.dto;

import ecommerce.common.response.PaginatedResponse;
import ecommerce.modules.wishlist.dto.WishlistItemDto;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter


public class WishListItemResponseDto {
    private List<WishlistItemDto> content;
    private PaginatedResponse<WishlistItemDto> pageInfo;

}

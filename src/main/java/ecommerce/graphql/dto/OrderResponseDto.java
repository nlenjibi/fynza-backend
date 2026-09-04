package ecommerce.graphql.dto;

import ecommerce.common.response.PaginatedResponse;
import ecommerce.modules.order.dto.OrderResponse;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class OrderResponseDto {
    private List<OrderResponse> content;
    private PaginatedResponse<OrderResponse> pageInfo;


}

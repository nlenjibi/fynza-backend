package ecommerce.graphql.dto;


import ecommerce.common.response.PaginatedResponse;
import ecommerce.modules.follow.dto.FollowedStoreResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowedStoreConnection {
    private List<FollowedStoreResponse> content;
    private PaginatedResponse<FollowedStoreResponse> pageInfo;
}

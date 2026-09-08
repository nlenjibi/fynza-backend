package ecommerce.graphql.dto;

import ecommerce.common.response.PaginatedResponse;
import ecommerce.modules.user.dto.UserProfileResponse;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class UserProfilePageDto {
    List<UserProfileResponse> content;
    PaginatedResponse<UserProfileResponse> pageInfo;
}

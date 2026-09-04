package ecommerce.graphql.dto;

import ecommerce.common.response.PaginatedResponse;
import ecommerce.modules.user.dto.UserDto;
import lombok.*;

import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class UserResponceDto {
    private List<UserDto> content;
    private PaginatedResponse<UserDto> pageInfo;

}

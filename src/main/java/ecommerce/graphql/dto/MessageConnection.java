package ecommerce.graphql.dto;

import ecommerce.common.response.PaginatedResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageConnection {
    private List<Object> content;
    private PaginatedResponse<Object> pageInfo;
}

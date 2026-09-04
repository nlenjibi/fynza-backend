package ecommerce.graphql.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkUserUpdateInput {
    private List<UUID> userIds;
    private String role;
    private Boolean isActive;
    private Boolean sendNotification = true;
}

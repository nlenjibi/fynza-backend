package ecommerce.modules.user.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Bulk User Update Request
 * Used for updating multiple users at once (activate, deactivate, role change)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUserUpdateRequest {

    @NotEmpty(message = "User IDs list cannot be empty")
    private List<UUID> userIds;

    private String role;  // Optional: new role to assign
    
    private Boolean isActive;  // Optional: new active status
    
    private Boolean sendNotification = true;  // Whether to send notification to users
}

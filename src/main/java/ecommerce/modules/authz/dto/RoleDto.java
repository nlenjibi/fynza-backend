package ecommerce.modules.authz.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder
public class RoleDto {
    UUID         id;
    String       code;
    String       displayName;
    String       description;
    boolean      systemDefined;
    boolean      active;
    List<String> permissions;
}

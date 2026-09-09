package ecommerce.modules.authz.dto;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class PermissionDto {
    UUID   id;
    String code;
    String resource;
    String action;
    String description;
}

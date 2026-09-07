package ecommerce.modules.audit.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuditActorInfo {
    private String name;
    private String email;
}

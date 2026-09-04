package ecommerce.graphql.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class SecurityStats {
    private int failedLoginAttempts;
    private String hitRate;
    private long accessLogSize;
    private int lockoutDurationMinutes;
}

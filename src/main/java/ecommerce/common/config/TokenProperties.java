package ecommerce.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.token")
public class TokenProperties {

    /** Access token lifetime in minutes. Default: 15 */
    private long accessMinutes = 15;

    /** Refresh token lifetime in days. Default: 7 */
    private long refreshDays = 7;

    /** Email verification token lifetime in hours. Default: 24 */
    private long emailVerificationHours = 24;

    /** Password reset token lifetime in minutes. Default: 15 */
    private long passwordResetMinutes = 15;

    /** MFA challenge token lifetime in minutes. Default: 5 */
    private long mfaChallengeMinutes = 5;

    public long accessMillis() {
        return accessMinutes * 60_000L;
    }

    public long refreshMillis() {
        return refreshDays * 86_400_000L;
    }
}

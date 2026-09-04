package ecommerce.modules.settings;

/**
 * Security rules for Settings module.
 * Defines authorization expressions for settings management.
 */
public class SettingsSecurityRules {

    /**
     * Admin-only access for sensitive settings.
     */
    public static final String ADMIN_ONLY = "hasRole('ADMIN')";

    /**
     * Public read access for general settings.
     */
    public static final String PUBLIC_READ = "permitAll()";

    /**
     * Authenticated users can view certain settings.
     */
    public static final String AUTHENTICATED = "isAuthenticated()";

    /**
     * Admin or seller access for business settings.
     */
    public static final String ADMIN_OR_SELLER = "hasAnyRole('ADMIN', 'SELLER')";

    private SettingsSecurityRules() {
        // Utility class
    }
}

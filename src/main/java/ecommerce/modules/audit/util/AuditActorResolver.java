package ecommerce.modules.audit.util;

import ecommerce.common.enums.Role;
import ecommerce.common.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Resolves the authenticated actor's identity from the Spring Security context
 * for constructing {@link ecommerce.modules.audit.dto.AuditLogEntry} instances.
 *
 * Fynza uses JWT (not sessions) — all identity is in the SecurityContextHolder.
 */
@Component
public class AuditActorResolver {

    public UUID resolveActorPublicId() {
        UserPrincipal p = principal();
        return p != null ? p.getPublicId() : null;
    }

    public String resolveActorEmail() {
        UserPrincipal p = principal();
        return p != null ? p.getEmail() : null;
    }

    public Role resolveActorRole() {
        UserPrincipal p = principal();
        return p != null ? p.getRole() : Role.CUSTOMER;
    }

    public String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private UserPrincipal principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        if (auth.getPrincipal() instanceof UserPrincipal p) return p;
        return null;
    }
}

package ecommerce.common.security;

import ecommerce.common.enums.Role;
import ecommerce.modules.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class UserPrincipal implements UserDetails {

    @Getter
    private final UUID id;          // UUID — the only identifier exposed in API and audit logs

    @Getter
    private final String email;

    private final String password;
    private final Boolean isActive;
    private final Boolean isLocked;

    @Getter
    private final Long lastPasswordChangeEpoch;

    @Getter
    private final Role role;

    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(User user) {
        this.id       = user.getId();
        this.email    = user.getEmail();
        this.password = user.getPassword();
        this.isActive = user.getIsActive();
        this.isLocked = user.getIsLocked();
        this.lastPasswordChangeEpoch = user.getLastPasswordChange() != null
                ? user.getLastPasswordChange()
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                : null;
        this.role        = user.getRole();
        this.authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
    }

    public static UserPrincipal create(User user) {
        return new UserPrincipal(user);
    }

    public boolean isAccountLocked() {
        return Boolean.TRUE.equals(isLocked);
    }

    // ── UserDetails ────────────────────────────────────────────────────────────

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String  getPassword()              { return password; }
    @Override public String  getUsername()              { return email; }
    @Override public boolean isAccountNonExpired()      { return true; }
    @Override public boolean isAccountNonLocked()       { return !Boolean.TRUE.equals(isLocked); }
    @Override public boolean isCredentialsNonExpired()  { return true; }
    @Override public boolean isEnabled()                { return Boolean.TRUE.equals(isActive); }
}

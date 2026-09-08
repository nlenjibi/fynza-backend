package ecommerce.modules.authz;

import ecommerce.common.security.SecurityRules;
import ecommerce.modules.authz.constant.FynzaPermissions;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class AuthzSecurityRules implements SecurityRules {

    @Override
    public void configure(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry.requestMatchers("/v1/admin/authz/**").hasAuthority(FynzaPermissions.ROLE_MANAGE);
    }
}

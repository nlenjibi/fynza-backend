package ecommerce.common.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class GraphqlSecurityRules implements SecurityRules {
    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                .requestMatchers("/graphql").permitAll()
                // GraphiQL IDE restricted to admins — schema introspection should not be public in production
                .requestMatchers("/graphiql", "/graphiql/**").hasRole("ADMIN");
    }
}

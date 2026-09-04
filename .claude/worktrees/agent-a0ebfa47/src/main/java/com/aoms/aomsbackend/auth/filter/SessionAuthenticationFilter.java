package com.aoms.aomsbackend.auth.filter;

import com.aoms.aomsbackend.auth.constant.SessionAttribute;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            populateSecurityContextFromSession(request);
        } finally {
            chain.doFilter(request, response);
        }
    }

    @SuppressWarnings("unchecked")
    private void populateSecurityContextFromSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }

        // Try V1 attributes (SSO) first, then V2 attributes (email/password)
        String userId = (String) session.getAttribute(SessionAttribute.USER_ID.getKey());
        if (userId == null) {
            userId = (String) session.getAttribute(SessionAttribute.V2_USER_ID.getKey());
        }

        if (userId == null) {
            return;
        }

        // Try V1 roles first, then V2 roles
        List<String> roles = (List<String>) session.getAttribute(SessionAttribute.ROLES.getKey());
        if (roles == null) {
            roles = (List<String>) session.getAttribute(SessionAttribute.V2_ROLES.getKey());
        }

        List<SimpleGrantedAuthority> authorities = roles == null
            ? List.of()
            : roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());

        UsernamePasswordAuthenticationToken auth =
            UsernamePasswordAuthenticationToken.authenticated(userId, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}

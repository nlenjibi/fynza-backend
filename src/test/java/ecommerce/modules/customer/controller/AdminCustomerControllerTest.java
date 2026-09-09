package ecommerce.modules.customer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ecommerce.common.config.RateLimitFilter;
import ecommerce.common.config.RateLimitProperties;
import ecommerce.common.exception.CustomAccessDeniedHandler;
import ecommerce.common.exception.CustomAuthenticationEntryPoint;
import ecommerce.common.security.JwtAuthenticationFilter;
import ecommerce.common.security.OAuth2AuthenticationFailureHandler;
import ecommerce.common.security.OAuth2AuthenticationSuccessHandler;
import ecommerce.common.security.UserPrincipal;
import ecommerce.common.util.CustomUserDetailsService;
import ecommerce.modules.customer.CustomerSecurityRules;
import ecommerce.modules.customer.dto.request.CustomerStatusRequest;
import ecommerce.modules.customer.dto.response.CustomerResponse;
import ecommerce.modules.customer.enums.CustomerStatus;
import ecommerce.modules.customer.exception.CustomerNotFoundException;
import ecommerce.modules.customer.service.CustomerStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCustomerController.class)
@ActiveProfiles("test")
@DisplayName("AdminCustomerController — /v1/admin/customers")
class AdminCustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private CustomerStatusService customerStatusService;

    // Security infra — required for the @WebMvcTest slice to load
    @MockBean private CustomUserDetailsService           customUserDetailsService;
    @MockBean private JwtAuthenticationFilter            jwtAuthenticationFilter;
    @MockBean private PasswordEncoder                    passwordEncoder;
    @MockBean private CustomAuthenticationEntryPoint     customAuthenticationEntryPoint;
    @MockBean private CustomAccessDeniedHandler          customAccessDeniedHandler;
    @MockBean private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    @MockBean private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    @MockBean private RateLimitFilter                    rateLimitFilter;
    @MockBean private RateLimitProperties                rateLimitProperties;
    @MockBean private JpaMetamodelMappingContext         jpaMetamodelMappingContext;

    private UUID          adminId;
    private UserPrincipal adminPrincipal;
    private UUID          customerId;

    @BeforeEach
    void setUp() throws Exception {
        adminId    = UUID.randomUUID();
        customerId = UUID.randomUUID();

        adminPrincipal = mock(UserPrincipal.class);
        when(adminPrincipal.getId()).thenReturn(adminId);
        when(adminPrincipal.getUsername()).thenReturn("admin@fynza.com");
        when(adminPrincipal.getPassword()).thenReturn("{noop}secret");
        List<GrantedAuthority> adminAuthorities = List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("customer.manage"),
                new SimpleGrantedAuthority("customer.suspend"),
                new SimpleGrantedAuthority("customer.activate"),
                new SimpleGrantedAuthority("customer.block"));
        doReturn(adminAuthorities).when(adminPrincipal).getAuthorities();
        when(adminPrincipal.isEnabled()).thenReturn(true);
        when(adminPrincipal.isAccountNonExpired()).thenReturn(true);
        when(adminPrincipal.isAccountNonLocked()).thenReturn(true);
        when(adminPrincipal.isCredentialsNonExpired()).thenReturn(true);

        doAnswer(new Answer<Void>() {
            @Override public Void answer(InvocationOnMock inv) throws Throwable {
                FilterChain chain = inv.getArgument(2);
                chain.doFilter((ServletRequest) inv.getArgument(0), (ServletResponse) inv.getArgument(1));
                return null;
            }
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());

        doAnswer(new Answer<Void>() {
            @Override public Void answer(InvocationOnMock inv) throws Throwable {
                FilterChain chain = inv.getArgument(2);
                chain.doFilter((ServletRequest) inv.getArgument(0), (ServletResponse) inv.getArgument(1));
                return null;
            }
        }).when(rateLimitFilter).doFilter(any(), any(), any());

        doAnswer(new Answer<Void>() {
            @Override public Void answer(InvocationOnMock inv) throws Throwable {
                ((HttpServletResponse) inv.getArgument(1)).sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return null;
            }
        }).when(customAuthenticationEntryPoint).commence(any(), any(), any());

        doAnswer(new Answer<Void>() {
            @Override public Void answer(InvocationOnMock inv) throws Throwable {
                ((HttpServletResponse) inv.getArgument(1)).sendError(HttpServletResponse.SC_FORBIDDEN);
                return null;
            }
        }).when(customAccessDeniedHandler).handle(any(), any(), any());
    }

    private CustomerResponse suspendedCustomerResponse() {
        return CustomerResponse.builder()
                .id(customerId)
                .customerNumber("CUS-000001")
                .status(CustomerStatus.SUSPENDED)
                .firstName("Alice")
                .lastName("Test")
                .email("alice@example.com")
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.now())
                .build();
    }

    // =========================================================================
    // PATCH /v1/admin/customers/{id}/suspend
    // =========================================================================

    @Nested
    @DisplayName("PATCH /v1/admin/customers/{id}/suspend")
    class SuspendCustomer {

        @Test
        @DisplayName("200 — admin with customer.suspend suspends the customer")
        void whenAuthorised_returns200WithSuspendedCustomer() throws Exception {
            CustomerStatusRequest req = new CustomerStatusRequest();
            req.setReason("Violation of terms");

            when(customerStatusService.suspendCustomer(eq(customerId), eq(adminId), any(CustomerStatusRequest.class)))
                    .thenReturn(suspendedCustomerResponse());

            mockMvc.perform(patch("/v1/admin/customers/{id}/suspend", customerId)
                            .with(user(adminPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Customer suspended"))
                    .andExpect(jsonPath("$.data.status").value("SUSPENDED"));

            verify(customerStatusService).suspendCustomer(eq(customerId), eq(adminId), any(CustomerStatusRequest.class));
        }

        @Test
        @DisplayName("401 — unauthenticated request is rejected")
        void whenUnauthenticated_returns401() throws Exception {
            mockMvc.perform(patch("/v1/admin/customers/{id}/suspend", customerId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 — user without customer.suspend permission is denied")
        void whenUnauthorised_returns403() throws Exception {
            UserPrincipal noPermission = buildPrincipalWithAuthorities(
                    List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

            mockMvc.perform(patch("/v1/admin/customers/{id}/suspend", customerId)
                            .with(user(noPermission))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // PATCH /v1/admin/customers/{id}/activate
    // =========================================================================

    @Nested
    @DisplayName("PATCH /v1/admin/customers/{id}/activate")
    class ActivateCustomer {

        @Test
        @DisplayName("200 — admin with customer.activate reactivates the customer")
        void whenAuthorised_returns200WithActiveCustomer() throws Exception {
            CustomerResponse activeResponse = CustomerResponse.builder()
                    .id(customerId)
                    .customerNumber("CUS-000001")
                    .status(CustomerStatus.ACTIVE)
                    .firstName("Alice")
                    .lastName("Test")
                    .email("alice@example.com")
                    .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                    .updatedAt(Instant.now())
                    .build();

            when(customerStatusService.activateCustomer(customerId, adminId)).thenReturn(activeResponse);

            mockMvc.perform(patch("/v1/admin/customers/{id}/activate", customerId)
                            .with(user(adminPrincipal))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Customer activated"))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"));

            verify(customerStatusService).activateCustomer(customerId, adminId);
        }

        @Test
        @DisplayName("401 — unauthenticated request is rejected")
        void whenUnauthenticated_returns401() throws Exception {
            mockMvc.perform(patch("/v1/admin/customers/{id}/activate", customerId)
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 — user without customer.activate permission is denied")
        void whenUnauthorised_returns403() throws Exception {
            UserPrincipal noPermission = buildPrincipalWithAuthorities(
                    List.of(new SimpleGrantedAuthority("ROLE_SELLER")));

            mockMvc.perform(patch("/v1/admin/customers/{id}/activate", customerId)
                            .with(user(noPermission))
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // PATCH /v1/admin/customers/{id}/block
    // =========================================================================

    @Nested
    @DisplayName("PATCH /v1/admin/customers/{id}/block")
    class BlockCustomer {

        @Test
        @DisplayName("200 — admin with customer.block blocks the customer")
        void whenAuthorised_returns200WithBlockedCustomer() throws Exception {
            CustomerStatusRequest req = new CustomerStatusRequest();
            req.setReason("Fraudulent activity");

            CustomerResponse blockedResponse = CustomerResponse.builder()
                    .id(customerId)
                    .customerNumber("CUS-000001")
                    .status(CustomerStatus.BLOCKED)
                    .firstName("Alice")
                    .lastName("Test")
                    .email("alice@example.com")
                    .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                    .updatedAt(Instant.now())
                    .build();

            when(customerStatusService.blockCustomer(eq(customerId), eq(adminId), any(CustomerStatusRequest.class)))
                    .thenReturn(blockedResponse);

            mockMvc.perform(patch("/v1/admin/customers/{id}/block", customerId)
                            .with(user(adminPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Customer blocked"))
                    .andExpect(jsonPath("$.data.status").value("BLOCKED"));

            verify(customerStatusService).blockCustomer(eq(customerId), eq(adminId), any(CustomerStatusRequest.class));
        }

        @Test
        @DisplayName("401 — unauthenticated request is rejected")
        void whenUnauthenticated_returns401() throws Exception {
            mockMvc.perform(patch("/v1/admin/customers/{id}/block", customerId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 — user without customer.block permission is denied")
        void whenUnauthorised_returns403() throws Exception {
            UserPrincipal noPermission = buildPrincipalWithAuthorities(
                    List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

            mockMvc.perform(patch("/v1/admin/customers/{id}/block", customerId)
                            .with(user(noPermission))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private UserPrincipal buildPrincipalWithAuthorities(List<GrantedAuthority> authorities) {
        UserPrincipal principal = mock(UserPrincipal.class);
        when(principal.getId()).thenReturn(UUID.randomUUID());
        when(principal.getUsername()).thenReturn("other@example.com");
        when(principal.getPassword()).thenReturn("{noop}pass");
        doReturn(authorities).when(principal).getAuthorities();
        when(principal.isEnabled()).thenReturn(true);
        when(principal.isAccountNonExpired()).thenReturn(true);
        when(principal.isAccountNonLocked()).thenReturn(true);
        when(principal.isCredentialsNonExpired()).thenReturn(true);
        return principal;
    }

    @TestConfiguration
    @Import(CustomerSecurityRules.class)
    static class TestSecurityConfig {

        @Autowired private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
        @Autowired private CustomAccessDeniedHandler      customAccessDeniedHandler;
        @Autowired private CustomerSecurityRules          customerSecurityRules;

        @Bean
        @Order(Integer.MIN_VALUE)
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                    .exceptionHandling(ex -> ex
                            .authenticationEntryPoint(customAuthenticationEntryPoint)
                            .accessDeniedHandler(customAccessDeniedHandler))
                    .authorizeHttpRequests(auth -> {
                        customerSecurityRules.configure(auth);
                        auth.anyRequest().authenticated();
                    })
                    .build();
        }
    }
}

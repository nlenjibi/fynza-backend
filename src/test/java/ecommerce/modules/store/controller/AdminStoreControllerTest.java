package ecommerce.modules.store.controller;

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
import ecommerce.modules.store.StoreSecurityRules;
import ecommerce.modules.store.dto.request.StoreStatusRequest;
import ecommerce.modules.store.dto.response.StoreDetailResponse;
import ecommerce.modules.store.dto.response.StoreResponse;
import ecommerce.modules.store.dto.response.StoreSummaryResponse;
import ecommerce.modules.store.enums.StoreStatus;
import ecommerce.modules.store.enums.StoreVisibility;
import ecommerce.modules.store.service.StoreManagementService;
import ecommerce.modules.store.service.StoreStatusService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminStoreController.class)
@ActiveProfiles("test")
@DisplayName("AdminStoreController Tests — /v1/admin/stores")
class AdminStoreControllerTest {

    @Autowired private MockMvc      mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private StoreManagementService storeManagementService;
    @MockBean private StoreStatusService     storeStatusService;

    // Security infra beans
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

    private UUID          sellerId;
    private UserPrincipal sellerPrincipal;

    @BeforeEach
    void setUp() throws Exception {
        adminId = UUID.randomUUID();
        adminPrincipal = mock(UserPrincipal.class);
        when(adminPrincipal.getId()).thenReturn(adminId);
        when(adminPrincipal.getUsername()).thenReturn("admin@example.com");
        when(adminPrincipal.getPassword()).thenReturn("{noop}secret");
        List<GrantedAuthority> adminAuthorities = List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("store.read"),
                new SimpleGrantedAuthority("store.suspend"),
                new SimpleGrantedAuthority("store.activate"),
                new SimpleGrantedAuthority("store.close")
        );
        doReturn(adminAuthorities).when(adminPrincipal).getAuthorities();
        when(adminPrincipal.isEnabled()).thenReturn(true);
        when(adminPrincipal.isAccountNonExpired()).thenReturn(true);
        when(adminPrincipal.isAccountNonLocked()).thenReturn(true);
        when(adminPrincipal.isCredentialsNonExpired()).thenReturn(true);

        sellerId = UUID.randomUUID();
        sellerPrincipal = mock(UserPrincipal.class);
        when(sellerPrincipal.getId()).thenReturn(sellerId);
        when(sellerPrincipal.getUsername()).thenReturn("seller@example.com");
        when(sellerPrincipal.getPassword()).thenReturn("{noop}secret");
        List<GrantedAuthority> sellerAuthorities = List.of(
                new SimpleGrantedAuthority("ROLE_SELLER"),
                new SimpleGrantedAuthority("store.read.own")
        );
        doReturn(sellerAuthorities).when(sellerPrincipal).getAuthorities();
        when(sellerPrincipal.isEnabled()).thenReturn(true);
        when(sellerPrincipal.isAccountNonExpired()).thenReturn(true);
        when(sellerPrincipal.isAccountNonLocked()).thenReturn(true);
        when(sellerPrincipal.isCredentialsNonExpired()).thenReturn(true);

        // Pass-through for all filters
        org.mockito.Mockito.doAnswer(new Answer<Void>() {
            @Override public Void answer(InvocationOnMock inv) throws Throwable {
                FilterChain chain = inv.getArgument(2);
                chain.doFilter((ServletRequest) inv.getArgument(0), (ServletResponse) inv.getArgument(1));
                return null;
            }
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());

        org.mockito.Mockito.doAnswer(new Answer<Void>() {
            @Override public Void answer(InvocationOnMock inv) throws Throwable {
                FilterChain chain = inv.getArgument(2);
                chain.doFilter((ServletRequest) inv.getArgument(0), (ServletResponse) inv.getArgument(1));
                return null;
            }
        }).when(rateLimitFilter).doFilter(any(), any(), any());

        org.mockito.Mockito.doAnswer(new Answer<Void>() {
            @Override public Void answer(InvocationOnMock inv) throws Throwable {
                ((HttpServletResponse) inv.getArgument(1)).sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return null;
            }
        }).when(customAuthenticationEntryPoint).commence(any(), any(), any());

        org.mockito.Mockito.doAnswer(new Answer<Void>() {
            @Override public Void answer(InvocationOnMock inv) throws Throwable {
                ((HttpServletResponse) inv.getArgument(1)).sendError(HttpServletResponse.SC_FORBIDDEN);
                return null;
            }
        }).when(customAccessDeniedHandler).handle(any(), any(), any());
    }

    // -- GET /v1/admin/stores --

    @Nested
    @DisplayName("GET /v1/admin/stores — searchStores")
    class SearchStores {

        @Test
        @DisplayName("200 — ADMIN with store.read retrieves list of stores")
        void whenAdmin_returns200() throws Exception {
            StoreSummaryResponse summary = StoreSummaryResponse.builder()
                    .id(UUID.randomUUID())
                    .storeName("Seller Store")
                    .slug("seller-store")
                    .status(StoreStatus.ACTIVE)
                    .visibility(StoreVisibility.PUBLIC)
                    .productCount(5L)
                    .avgRating(4.2)
                    .reviewCount(15L)
                    .orderCount(30L)
                    .createdAt(Instant.now())
                    .build();

            Page<StoreSummaryResponse> page = new PageImpl<>(List.of(summary));
            when(storeManagementService.searchStores(any(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/v1/admin/stores")
                            .with(user(adminPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].storeName").value("Seller Store"));
        }

        @Test
        @DisplayName("403 — non-admin (SELLER role) is forbidden")
        void whenNonAdmin_returns403() throws Exception {
            mockMvc.perform(get("/v1/admin/stores")
                            .with(user(sellerPrincipal)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("401 — unauthenticated request is rejected")
        void whenUnauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/v1/admin/stores"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -- GET /v1/admin/stores/{storeId} --

    @Nested
    @DisplayName("GET /v1/admin/stores/{storeId} — getStore")
    class GetStore {

        @Test
        @DisplayName("200 — ADMIN with store.read retrieves store details")
        void whenAdmin_returns200() throws Exception {
            UUID storeId = UUID.randomUUID();
            StoreDetailResponse response = StoreDetailResponse.builder()
                    .id(storeId)
                    .storeName("Detailed Store")
                    .slug("detailed-store")
                    .status(StoreStatus.ACTIVE)
                    .visibility(StoreVisibility.PUBLIC)
                    .policies(List.of())
                    .build();

            when(storeManagementService.getStoreByPublicId(storeId)).thenReturn(response);

            mockMvc.perform(get("/v1/admin/stores/{storeId}", storeId)
                            .with(user(adminPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.storeName").value("Detailed Store"));
        }

        @Test
        @DisplayName("403 — non-admin is forbidden")
        void whenNonAdmin_returns403() throws Exception {
            UUID storeId = UUID.randomUUID();
            mockMvc.perform(get("/v1/admin/stores/{storeId}", storeId)
                            .with(user(sellerPrincipal)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("401 — unauthenticated request is rejected")
        void whenUnauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/v1/admin/stores/{storeId}", UUID.randomUUID()))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -- PATCH /v1/admin/stores/{storeId}/status --

    @Nested
    @DisplayName("PATCH /v1/admin/stores/{storeId}/status — changeStatus")
    class ChangeStatus {

        @Test
        @DisplayName("200 — ADMIN with store.activate changes status to ACTIVE")
        void whenAdmin_returns200() throws Exception {
            UUID storeId = UUID.randomUUID();
            StoreStatusRequest req = new StoreStatusRequest();
            req.setStatus(StoreStatus.ACTIVE);
            req.setReason("Approved after review");

            StoreResponse response = StoreResponse.builder()
                    .id(storeId)
                    .status(StoreStatus.ACTIVE)
                    .storeName("Approved Store")
                    .build();

            when(storeStatusService.changeStatus(eq(storeId), eq(adminId), any(StoreStatusRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(patch("/v1/admin/stores/{storeId}/status", storeId)
                            .with(user(adminPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.storeName").value("Approved Store"));
        }

        @Test
        @DisplayName("403 — non-admin is forbidden from changing status")
        void whenNonAdmin_returns403() throws Exception {
            UUID storeId = UUID.randomUUID();
            StoreStatusRequest req = new StoreStatusRequest();
            req.setStatus(StoreStatus.ACTIVE);

            mockMvc.perform(patch("/v1/admin/stores/{storeId}/status", storeId)
                            .with(user(sellerPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("401 — unauthenticated request is rejected")
        void whenUnauthenticated_returns401() throws Exception {
            StoreStatusRequest req = new StoreStatusRequest();
            req.setStatus(StoreStatus.ACTIVE);

            mockMvc.perform(patch("/v1/admin/stores/{storeId}/status", UUID.randomUUID())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @TestConfiguration
    @Import(StoreSecurityRules.class)
    static class TestSecurityConfig {

        @Autowired private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
        @Autowired private CustomAccessDeniedHandler      customAccessDeniedHandler;
        @Autowired private StoreSecurityRules             storeSecurityRules;

        @Bean
        @Order(Integer.MIN_VALUE)
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                    .exceptionHandling(ex -> ex
                            .authenticationEntryPoint(customAuthenticationEntryPoint)
                            .accessDeniedHandler(customAccessDeniedHandler))
                    .authorizeHttpRequests(auth -> {
                        storeSecurityRules.configure(auth);
                        auth.anyRequest().authenticated();
                    })
                    .build();
        }
    }
}

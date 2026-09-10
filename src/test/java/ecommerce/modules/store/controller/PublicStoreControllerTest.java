package ecommerce.modules.store.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ecommerce.common.config.RateLimitFilter;
import ecommerce.common.config.RateLimitProperties;
import ecommerce.common.exception.CustomAccessDeniedHandler;
import ecommerce.common.exception.CustomAuthenticationEntryPoint;
import ecommerce.common.security.JwtAuthenticationFilter;
import ecommerce.common.security.OAuth2AuthenticationFailureHandler;
import ecommerce.common.security.OAuth2AuthenticationSuccessHandler;
import ecommerce.common.util.CustomUserDetailsService;
import ecommerce.modules.store.StoreSecurityRules;
import ecommerce.modules.store.dto.response.StoreSummaryResponse;
import ecommerce.modules.store.enums.StoreStatus;
import ecommerce.modules.store.enums.StoreVisibility;
import ecommerce.modules.store.service.StoreManagementService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicStoreController.class)
@ActiveProfiles("test")
@DisplayName("PublicStoreController Tests — /v1/stores")
class PublicStoreControllerTest {

    @Autowired private MockMvc      mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private StoreManagementService storeManagementService;

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

    @BeforeEach
    void setUp() throws Exception {
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
    }

    // -- GET /v1/stores --

    @Nested
    @DisplayName("GET /v1/stores — searchStores")
    class SearchStores {

        @Test
        @DisplayName("200 — returns page of stores without authentication (public endpoint)")
        void whenNoAuth_returns200WithStores() throws Exception {
            StoreSummaryResponse summary = StoreSummaryResponse.builder()
                    .id(UUID.randomUUID())
                    .storeName("Public Store")
                    .slug("public-store")
                    .status(StoreStatus.ACTIVE)
                    .visibility(StoreVisibility.PUBLIC)
                    .productCount(10L)
                    .avgRating(4.5)
                    .reviewCount(20L)
                    .orderCount(50L)
                    .createdAt(Instant.now())
                    .build();

            Page<StoreSummaryResponse> page = new PageImpl<>(List.of(summary));
            when(storeManagementService.searchStores(any(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/v1/stores"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].storeName").value("Public Store"));
        }

        @Test
        @DisplayName("200 — empty page is returned when no stores match")
        void whenNoStoresMatch_returns200WithEmptyPage() throws Exception {
            Page<StoreSummaryResponse> emptyPage = new PageImpl<>(List.of());
            when(storeManagementService.searchStores(any(), any(Pageable.class))).thenReturn(emptyPage);

            mockMvc.perform(get("/v1/stores"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isEmpty());
        }
    }

    // -- GET /v1/stores/{slug} --

    @Nested
    @DisplayName("GET /v1/stores/{slug} — getStoreBySlug")
    class GetStoreBySlug {

        @Test
        @DisplayName("200 — returns store by slug without authentication")
        void whenNoAuth_returns200WithStore() throws Exception {
            StoreSummaryResponse summary = StoreSummaryResponse.builder()
                    .id(UUID.randomUUID())
                    .storeName("Test Store")
                    .slug("test-store")
                    .status(StoreStatus.ACTIVE)
                    .visibility(StoreVisibility.PUBLIC)
                    .productCount(5L)
                    .avgRating(4.0)
                    .reviewCount(10L)
                    .orderCount(25L)
                    .createdAt(Instant.now())
                    .build();

            when(storeManagementService.getStoreBySlug("test-store")).thenReturn(summary);

            mockMvc.perform(get("/v1/stores/{slug}", "test-store"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.slug").value("test-store"))
                    .andExpect(jsonPath("$.data.storeName").value("Test Store"));
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

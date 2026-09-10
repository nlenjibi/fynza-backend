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
import ecommerce.modules.store.dto.request.CreateStoreRequest;
import ecommerce.modules.store.dto.request.StorePolicyRequest;
import ecommerce.modules.store.dto.request.StoreSettingRequest;
import ecommerce.modules.store.dto.request.StoreVisibilityRequest;
import ecommerce.modules.store.dto.request.UpdateStoreRequest;
import ecommerce.modules.store.dto.response.StoreDetailResponse;
import ecommerce.modules.store.dto.response.StorePolicyResponse;
import ecommerce.modules.store.dto.response.StoreResponse;
import ecommerce.modules.store.dto.response.StoreSettingResponse;
import ecommerce.modules.store.enums.StorePolicyStatus;
import ecommerce.modules.store.enums.StorePolicyType;
import ecommerce.modules.store.enums.StoreStatus;
import ecommerce.modules.store.enums.StoreVisibility;
import ecommerce.modules.store.service.StorePolicyService;
import ecommerce.modules.store.service.StoreManagementService;
import ecommerce.modules.store.service.StoreSettingService;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SellerStoreController.class)
@ActiveProfiles("test")
@DisplayName("SellerStoreController Tests — /v1/sellers/me/stores")
class SellerStoreControllerTest {

    @Autowired private MockMvc       mockMvc;
    @Autowired private ObjectMapper  objectMapper;

    @MockBean private StoreManagementService storeManagementService;
    @MockBean private StoreStatusService     storeStatusService;
    @MockBean private StoreSettingService    storeSettingService;
    @MockBean private StorePolicyService     storePolicyService;

    // Security infra beans required by the @WebMvcTest slice
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

    private UUID          principalId;
    private UserPrincipal mockPrincipal;

    @BeforeEach
    void setUp() throws Exception {
        principalId = UUID.randomUUID();

        mockPrincipal = mock(UserPrincipal.class);
        when(mockPrincipal.getId()).thenReturn(principalId);
        when(mockPrincipal.getUsername()).thenReturn("seller@example.com");
        when(mockPrincipal.getPassword()).thenReturn("{noop}secret");
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_SELLER"),
                new SimpleGrantedAuthority("store.create.own"),
                new SimpleGrantedAuthority("store.read.own"),
                new SimpleGrantedAuthority("store.update.own"),
                new SimpleGrantedAuthority("store.publish.own"),
                new SimpleGrantedAuthority("store.manage.own"),
                new SimpleGrantedAuthority("store.pause.own"),
                new SimpleGrantedAuthority("store.delete.own")
        );
        doReturn(authorities).when(mockPrincipal).getAuthorities();
        when(mockPrincipal.isEnabled()).thenReturn(true);
        when(mockPrincipal.isAccountNonExpired()).thenReturn(true);
        when(mockPrincipal.isAccountNonLocked()).thenReturn(true);
        when(mockPrincipal.isCredentialsNonExpired()).thenReturn(true);

        // Pass-through for all filters
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

    // -- POST /v1/sellers/me/stores --

    @Nested
    @DisplayName("POST /v1/sellers/me/stores — createStore")
    class CreateStore {

        @Test
        @DisplayName("201 — authenticated seller with store.create.own creates store")
        void whenAuthenticatedWithPermission_returns201() throws Exception {
            CreateStoreRequest req = new CreateStoreRequest();
            req.setStoreName("New Store");
            req.setBusinessEmail("store@example.com");

            StoreDetailResponse response = StoreDetailResponse.builder()
                    .id(UUID.randomUUID())
                    .storeName("New Store")
                    .status(StoreStatus.DRAFT)
                    .policies(List.of())
                    .build();

            when(storeManagementService.createStore(eq(principalId), any(CreateStoreRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/v1/sellers/me/stores")
                            .with(user(mockPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.storeName").value("New Store"));
        }

        @Test
        @DisplayName("401 — unauthenticated request is rejected")
        void whenUnauthenticated_returns401() throws Exception {
            CreateStoreRequest req = new CreateStoreRequest();
            req.setStoreName("New Store");

            mockMvc.perform(post("/v1/sellers/me/stores")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -- GET /v1/sellers/me/stores/me --

    @Nested
    @DisplayName("GET /v1/sellers/me/stores/me — getMyStore")
    class GetMyStore {

        @Test
        @DisplayName("200 — authenticated seller retrieves their store")
        void whenAuthenticated_returns200() throws Exception {
            StoreDetailResponse response = StoreDetailResponse.builder()
                    .id(UUID.randomUUID())
                    .storeName("My Store")
                    .status(StoreStatus.ACTIVE)
                    .policies(List.of())
                    .build();

            when(storeManagementService.getMyStore(principalId)).thenReturn(response);

            mockMvc.perform(get("/v1/sellers/me/stores/me")
                            .with(user(mockPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.storeName").value("My Store"));
        }

        @Test
        @DisplayName("401 — unauthenticated request is rejected")
        void whenUnauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/v1/sellers/me/stores/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -- PATCH /v1/sellers/me/stores/me --

    @Nested
    @DisplayName("PATCH /v1/sellers/me/stores/me — updateStore")
    class UpdateStore {

        @Test
        @DisplayName("200 — authenticated seller updates their store")
        void whenAuthenticated_returns200() throws Exception {
            UpdateStoreRequest req = new UpdateStoreRequest();
            req.setStoreName("Updated Store");

            StoreResponse response = StoreResponse.builder()
                    .id(UUID.randomUUID())
                    .storeName("Updated Store")
                    .build();

            when(storeManagementService.updateStore(eq(principalId), any(UpdateStoreRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(patch("/v1/sellers/me/stores/me")
                            .with(user(mockPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.storeName").value("Updated Store"));
        }

        @Test
        @DisplayName("401 — unauthenticated request is rejected")
        void whenUnauthenticated_returns401() throws Exception {
            mockMvc.perform(patch("/v1/sellers/me/stores/me")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -- POST /v1/sellers/me/stores/me/submit --

    @Nested
    @DisplayName("POST /v1/sellers/me/stores/me/submit — submitForReview")
    class SubmitForReview {

        @Test
        @DisplayName("200 — authenticated seller submits store for review")
        void whenAuthenticated_returns200() throws Exception {
            StoreResponse response = StoreResponse.builder()
                    .id(UUID.randomUUID())
                    .status(StoreStatus.PENDING_REVIEW)
                    .build();

            when(storeManagementService.submitForReview(principalId)).thenReturn(response);

            mockMvc.perform(post("/v1/sellers/me/stores/me/submit")
                            .with(user(mockPrincipal))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("401 — unauthenticated request is rejected")
        void whenUnauthenticated_returns401() throws Exception {
            mockMvc.perform(post("/v1/sellers/me/stores/me/submit")
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -- PATCH /v1/sellers/me/stores/me/visibility --

    @Nested
    @DisplayName("PATCH /v1/sellers/me/stores/me/visibility — updateVisibility")
    class UpdateVisibility {

        @Test
        @DisplayName("200 — authenticated seller updates visibility")
        void whenAuthenticated_returns200() throws Exception {
            StoreVisibilityRequest req = new StoreVisibilityRequest();
            req.setVisibility(StoreVisibility.PUBLIC);

            StoreResponse response = StoreResponse.builder()
                    .id(UUID.randomUUID())
                    .visibility(StoreVisibility.PUBLIC)
                    .build();

            when(storeManagementService.updateVisibility(eq(principalId), any(StoreVisibilityRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(patch("/v1/sellers/me/stores/me/visibility")
                            .with(user(mockPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("401 — unauthenticated request is rejected")
        void whenUnauthenticated_returns401() throws Exception {
            mockMvc.perform(patch("/v1/sellers/me/stores/me/visibility")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -- GET /v1/sellers/me/stores/me/settings --

    @Nested
    @DisplayName("GET /v1/sellers/me/stores/me/settings — getSettings")
    class GetSettings {

        @Test
        @DisplayName("200 — authenticated seller retrieves settings")
        void whenAuthenticated_returns200() throws Exception {
            StoreSettingResponse response = StoreSettingResponse.builder()
                    .id(UUID.randomUUID())
                    .currency("GHS")
                    .timezone("Africa/Accra")
                    .language("en")
                    .orderNotifications(true)
                    .customerNotifications(true)
                    .updatedAt(Instant.now())
                    .build();

            when(storeSettingService.getSettings(principalId)).thenReturn(response);

            mockMvc.perform(get("/v1/sellers/me/stores/me/settings")
                            .with(user(mockPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.currency").value("GHS"));
        }

        @Test
        @DisplayName("401 — unauthenticated request is rejected")
        void whenUnauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/v1/sellers/me/stores/me/settings"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -- PATCH /v1/sellers/me/stores/me/settings --

    @Nested
    @DisplayName("PATCH /v1/sellers/me/stores/me/settings — updateSettings")
    class UpdateSettings {

        @Test
        @DisplayName("200 — authenticated seller updates settings")
        void whenAuthenticated_returns200() throws Exception {
            StoreSettingRequest req = new StoreSettingRequest();
            req.setCurrency("USD");

            StoreSettingResponse response = StoreSettingResponse.builder()
                    .id(UUID.randomUUID())
                    .currency("USD")
                    .updatedAt(Instant.now())
                    .build();

            when(storeSettingService.updateSettings(eq(principalId), any(StoreSettingRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(patch("/v1/sellers/me/stores/me/settings")
                            .with(user(mockPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.currency").value("USD"));
        }

        @Test
        @DisplayName("401 — unauthenticated request is rejected")
        void whenUnauthenticated_returns401() throws Exception {
            mockMvc.perform(patch("/v1/sellers/me/stores/me/settings")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -- GET /v1/sellers/me/stores/me/policies --

    @Nested
    @DisplayName("GET /v1/sellers/me/stores/me/policies — getPolicies")
    class GetPolicies {

        @Test
        @DisplayName("200 — authenticated seller retrieves policies")
        void whenAuthenticated_returns200() throws Exception {
            StorePolicyResponse policyResponse = StorePolicyResponse.builder()
                    .id(UUID.randomUUID())
                    .type(StorePolicyType.RETURN)
                    .title("Return Policy")
                    .version(1)
                    .status(StorePolicyStatus.DRAFT)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(storePolicyService.getPolicies(principalId)).thenReturn(List.of(policyResponse));

            mockMvc.perform(get("/v1/sellers/me/stores/me/policies")
                            .with(user(mockPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].title").value("Return Policy"));
        }

        @Test
        @DisplayName("401 — unauthenticated request is rejected")
        void whenUnauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/v1/sellers/me/stores/me/policies"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -- POST /v1/sellers/me/stores/me/policies --

    @Nested
    @DisplayName("POST /v1/sellers/me/stores/me/policies — createPolicy")
    class CreatePolicy {

        @Test
        @DisplayName("201 — authenticated seller creates policy")
        void whenAuthenticated_returns201() throws Exception {
            StorePolicyRequest req = new StorePolicyRequest();
            req.setType(StorePolicyType.RETURN);
            req.setTitle("Return Policy");
            req.setContent("30-day return policy.");

            StorePolicyResponse response = StorePolicyResponse.builder()
                    .id(UUID.randomUUID())
                    .type(StorePolicyType.RETURN)
                    .title("Return Policy")
                    .version(1)
                    .status(StorePolicyStatus.DRAFT)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(storePolicyService.createPolicy(eq(principalId), any(StorePolicyRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/v1/sellers/me/stores/me/policies")
                            .with(user(mockPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("Return Policy"));
        }

        @Test
        @DisplayName("401 — unauthenticated request is rejected")
        void whenUnauthenticated_returns401() throws Exception {
            mockMvc.perform(post("/v1/sellers/me/stores/me/policies")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -- PATCH /v1/sellers/me/stores/me/policies/{policyId} --

    @Nested
    @DisplayName("PATCH /v1/sellers/me/stores/me/policies/{policyId} — updatePolicy")
    class UpdatePolicy {

        @Test
        @DisplayName("200 — authenticated seller updates a policy")
        void whenAuthenticated_returns200() throws Exception {
            UUID policyId = UUID.randomUUID();
            StorePolicyRequest req = new StorePolicyRequest();
            req.setType(StorePolicyType.RETURN);
            req.setTitle("Updated Policy");
            req.setContent("Updated content.");

            StorePolicyResponse response = StorePolicyResponse.builder()
                    .id(policyId)
                    .title("Updated Policy")
                    .version(2)
                    .status(StorePolicyStatus.ACTIVE)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(storePolicyService.updatePolicy(eq(principalId), eq(policyId), any(StorePolicyRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(patch("/v1/sellers/me/stores/me/policies/{policyId}", policyId)
                            .with(user(mockPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("Updated Policy"));
        }

        @Test
        @DisplayName("401 — unauthenticated request is rejected")
        void whenUnauthenticated_returns401() throws Exception {
            mockMvc.perform(patch("/v1/sellers/me/stores/me/policies/{policyId}", UUID.randomUUID())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -- DELETE /v1/sellers/me/stores/me/policies/{policyId} --

    @Nested
    @DisplayName("DELETE /v1/sellers/me/stores/me/policies/{policyId} — deletePolicy")
    class DeletePolicy {

        @Test
        @DisplayName("200 — authenticated seller deletes a policy")
        void whenAuthenticated_returns200() throws Exception {
            UUID policyId = UUID.randomUUID();
            doNothing().when(storePolicyService).deletePolicy(principalId, policyId);

            mockMvc.perform(delete("/v1/sellers/me/stores/me/policies/{policyId}", policyId)
                            .with(user(mockPrincipal))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Policy deleted"));
        }

        @Test
        @DisplayName("401 — unauthenticated request is rejected")
        void whenUnauthenticated_returns401() throws Exception {
            mockMvc.perform(delete("/v1/sellers/me/stores/me/policies/{policyId}", UUID.randomUUID())
                            .with(csrf()))
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

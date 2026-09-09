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
import ecommerce.modules.customer.dto.request.CustomerAddressRequest;
import ecommerce.modules.customer.dto.request.CustomerPreferenceRequest;
import ecommerce.modules.customer.dto.request.CustomerUpdateRequest;
import ecommerce.modules.customer.dto.response.CustomerAddressResponse;
import ecommerce.modules.customer.dto.response.CustomerPreferenceResponse;
import ecommerce.modules.customer.dto.response.CustomerResponse;
import ecommerce.modules.customer.enums.CustomerAddressType;
import ecommerce.modules.customer.enums.CustomerStatus;
import ecommerce.modules.customer.service.CustomerAddressService;
import ecommerce.modules.customer.service.CustomerPreferenceService;
import ecommerce.modules.customer.service.CustomerService;
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
import org.springframework.security.test.context.support.WithMockUser;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerMeController.class)
@ActiveProfiles("test")
@DisplayName("CustomerMeController — /v1/customers/me")
class CustomerMeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private CustomerService           customerService;
    @MockBean private CustomerAddressService    addressService;
    @MockBean private CustomerPreferenceService preferenceService;

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

    private UUID            principalId;
    private UserPrincipal   mockPrincipal;
    private CustomerResponse sampleCustomerResponse;

    @BeforeEach
    void setUp() throws Exception {
        principalId = UUID.randomUUID();

        mockPrincipal = mock(UserPrincipal.class);
        when(mockPrincipal.getId()).thenReturn(principalId);
        when(mockPrincipal.getUsername()).thenReturn("alice@example.com");
        when(mockPrincipal.getPassword()).thenReturn("{noop}secret");
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_CUSTOMER"),
                new SimpleGrantedAuthority("customer.update.own"),
                new SimpleGrantedAuthority("address.create.own"),
                new SimpleGrantedAuthority("address.update.own"),
                new SimpleGrantedAuthority("address.delete.own"));
        doReturn(authorities).when(mockPrincipal).getAuthorities();
        when(mockPrincipal.isEnabled()).thenReturn(true);
        when(mockPrincipal.isAccountNonExpired()).thenReturn(true);
        when(mockPrincipal.isAccountNonLocked()).thenReturn(true);
        when(mockPrincipal.isCredentialsNonExpired()).thenReturn(true);

        sampleCustomerResponse = CustomerResponse.builder()
                .id(UUID.randomUUID())
                .customerNumber("CUS-000001")
                .status(CustomerStatus.ACTIVE)
                .firstName("Alice")
                .lastName("Test")
                .email("alice@example.com")
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-06-01T00:00:00Z"))
                .build();

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

    // =========================================================================
    // PATCH /v1/customers/me
    // =========================================================================

    @Nested
    @DisplayName("PATCH /v1/customers/me")
    class UpdateMyCustomer {

        @Test
        @DisplayName("200 — authenticated request updates profile and returns CustomerResponse")
        void whenAuthenticated_returns200WithUpdatedProfile() throws Exception {
            CustomerUpdateRequest req = new CustomerUpdateRequest();
            req.setFirstName("Alice");
            req.setLastName("Updated");

            when(customerService.updateMyCustomer(eq(principalId), any(CustomerUpdateRequest.class)))
                    .thenReturn(sampleCustomerResponse);

            mockMvc.perform(patch("/v1/customers/me")
                            .with(user(mockPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Customer profile updated"))
                    .andExpect(jsonPath("$.data.customerNumber").value("CUS-000001"));

            verify(customerService).updateMyCustomer(eq(principalId), any(CustomerUpdateRequest.class));
        }

        @Test
        @DisplayName("401 — unauthenticated request is rejected")
        void whenUnauthenticated_returns401() throws Exception {
            CustomerUpdateRequest req = new CustomerUpdateRequest();
            req.setFirstName("Alice");

            mockMvc.perform(patch("/v1/customers/me")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("400 — firstName exceeding 100 chars is rejected")
        void whenFirstNameTooLong_returns400() throws Exception {
            CustomerUpdateRequest req = new CustomerUpdateRequest();
            req.setFirstName("A".repeat(101));

            mockMvc.perform(patch("/v1/customers/me")
                            .with(user(mockPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // PATCH /v1/customers/me/preferences
    // =========================================================================

    @Nested
    @DisplayName("PATCH /v1/customers/me/preferences")
    class UpdatePreferences {

        @Test
        @DisplayName("200 — updates preferences and returns PreferenceResponse")
        void whenAuthenticated_returns200WithPreferences() throws Exception {
            CustomerPreferenceRequest req = new CustomerPreferenceRequest();
            req.setLanguage("en");
            req.setCurrency("GHS");
            req.setEmailNotifications(true);

            CustomerPreferenceResponse prefResponse = CustomerPreferenceResponse.builder()
                    .id(UUID.randomUUID())
                    .language("en")
                    .currency("GHS")
                    .emailNotifications(true)
                    .smsNotifications(false)
                    .pushNotifications(false)
                    .marketingOptIn(false)
                    .updatedAt(Instant.now())
                    .build();

            when(preferenceService.updatePreferences(eq(principalId), any(CustomerPreferenceRequest.class)))
                    .thenReturn(prefResponse);

            mockMvc.perform(patch("/v1/customers/me/preferences")
                            .with(user(mockPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Preferences updated"))
                    .andExpect(jsonPath("$.data.language").value("en"))
                    .andExpect(jsonPath("$.data.currency").value("GHS"));

            verify(preferenceService).updatePreferences(eq(principalId), any(CustomerPreferenceRequest.class));
        }

        @Test
        @DisplayName("401 — unauthenticated request is rejected")
        void whenUnauthenticated_returns401() throws Exception {
            mockMvc.perform(patch("/v1/customers/me/preferences")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("400 — currency with wrong length is rejected")
        void whenCurrencyWrongLength_returns400() throws Exception {
            CustomerPreferenceRequest req = new CustomerPreferenceRequest();
            req.setCurrency("GH"); // must be exactly 3 chars

            mockMvc.perform(patch("/v1/customers/me/preferences")
                            .with(user(mockPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // POST /v1/customers/me/addresses
    // =========================================================================

    @Nested
    @DisplayName("POST /v1/customers/me/addresses")
    class AddAddress {

        private CustomerAddressRequest validAddressRequest() {
            CustomerAddressRequest req = new CustomerAddressRequest();
            req.setRecipientName("Alice Test");
            req.setAddressLine1("123 Main St");
            req.setCity("Accra");
            req.setCountry("Ghana");
            req.setAddressType(CustomerAddressType.HOME);
            req.setIsDefault(true);
            return req;
        }

        @Test
        @DisplayName("201 — valid address is created and returned")
        void whenValidRequest_returns201WithAddress() throws Exception {
            CustomerAddressResponse addressResponse = CustomerAddressResponse.builder()
                    .id(UUID.randomUUID())
                    .recipientName("Alice Test")
                    .addressLine1("123 Main St")
                    .city("Accra")
                    .country("Ghana")
                    .addressType(CustomerAddressType.HOME)
                    .isDefault(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(addressService.addAddress(eq(principalId), any(CustomerAddressRequest.class)))
                    .thenReturn(addressResponse);

            mockMvc.perform(post("/v1/customers/me/addresses")
                            .with(user(mockPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validAddressRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Address added"))
                    .andExpect(jsonPath("$.data.city").value("Accra"));

            verify(addressService).addAddress(eq(principalId), any(CustomerAddressRequest.class));
        }

        @Test
        @DisplayName("401 — unauthenticated request is rejected")
        void whenUnauthenticated_returns401() throws Exception {
            mockMvc.perform(post("/v1/customers/me/addresses")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validAddressRequest())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("400 — missing recipientName fails @NotBlank")
        void whenRecipientNameMissing_returns400() throws Exception {
            CustomerAddressRequest req = validAddressRequest();
            req.setRecipientName(null);

            mockMvc.perform(post("/v1/customers/me/addresses")
                            .with(user(mockPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 — missing addressLine1 fails @NotBlank")
        void whenAddressLine1Missing_returns400() throws Exception {
            CustomerAddressRequest req = validAddressRequest();
            req.setAddressLine1(null);

            mockMvc.perform(post("/v1/customers/me/addresses")
                            .with(user(mockPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 — missing city fails @NotBlank")
        void whenCityMissing_returns400() throws Exception {
            CustomerAddressRequest req = validAddressRequest();
            req.setCity(null);

            mockMvc.perform(post("/v1/customers/me/addresses")
                            .with(user(mockPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 — missing country fails @NotBlank")
        void whenCountryMissing_returns400() throws Exception {
            CustomerAddressRequest req = validAddressRequest();
            req.setCountry(null);

            mockMvc.perform(post("/v1/customers/me/addresses")
                            .with(user(mockPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // PATCH /v1/customers/me/addresses/{addressId}
    // =========================================================================

    @Nested
    @DisplayName("PATCH /v1/customers/me/addresses/{addressId}")
    class UpdateAddress {

        @Test
        @DisplayName("200 — valid update returns updated address")
        void whenValidRequest_returns200() throws Exception {
            UUID addressId = UUID.randomUUID();
            CustomerAddressRequest req = new CustomerAddressRequest();
            req.setRecipientName("Bob Updated");
            req.setAddressLine1("456 Other St");
            req.setCity("Kumasi");
            req.setCountry("Ghana");

            CustomerAddressResponse response = CustomerAddressResponse.builder()
                    .id(addressId)
                    .recipientName("Bob Updated")
                    .city("Kumasi")
                    .country("Ghana")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(addressService.updateAddress(eq(principalId), eq(addressId), any(CustomerAddressRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(patch("/v1/customers/me/addresses/{addressId}", addressId)
                            .with(user(mockPrincipal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Address updated"))
                    .andExpect(jsonPath("$.data.city").value("Kumasi"));
        }

        @Test
        @DisplayName("401 — unauthenticated request is rejected")
        void whenUnauthenticated_returns401() throws Exception {
            UUID addressId = UUID.randomUUID();
            mockMvc.perform(patch("/v1/customers/me/addresses/{addressId}", addressId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // DELETE /v1/customers/me/addresses/{addressId}
    // =========================================================================

    @Nested
    @DisplayName("DELETE /v1/customers/me/addresses/{addressId}")
    class DeleteAddress {

        @Test
        @DisplayName("200 — authenticated delete returns success with null data")
        void whenAuthenticated_returns200() throws Exception {
            UUID addressId = UUID.randomUUID();
            doNothing().when(addressService).deleteAddress(principalId, addressId);

            mockMvc.perform(delete("/v1/customers/me/addresses/{addressId}", addressId)
                            .with(user(mockPrincipal))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Address deleted"));

            verify(addressService).deleteAddress(principalId, addressId);
        }

        @Test
        @DisplayName("401 — unauthenticated request is rejected")
        void whenUnauthenticated_returns401() throws Exception {
            UUID addressId = UUID.randomUUID();
            mockMvc.perform(delete("/v1/customers/me/addresses/{addressId}", addressId)
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // POST /v1/customers/me/addresses/{addressId}/default
    // =========================================================================

    @Nested
    @DisplayName("POST /v1/customers/me/addresses/{addressId}/default")
    class SetDefaultAddress {

        @Test
        @DisplayName("200 — sets address as default and returns updated address")
        void whenAuthenticated_returns200() throws Exception {
            UUID addressId = UUID.randomUUID();
            CustomerAddressResponse response = CustomerAddressResponse.builder()
                    .id(addressId)
                    .recipientName("Alice")
                    .city("Accra")
                    .country("Ghana")
                    .isDefault(true)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(addressService.setDefaultAddress(principalId, addressId)).thenReturn(response);

            mockMvc.perform(post("/v1/customers/me/addresses/{addressId}/default", addressId)
                            .with(user(mockPrincipal))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Default address set"))
                    .andExpect(jsonPath("$.data.isDefault").value(true));
        }

        @Test
        @DisplayName("401 — unauthenticated request is rejected")
        void whenUnauthenticated_returns401() throws Exception {
            UUID addressId = UUID.randomUUID();
            mockMvc.perform(post("/v1/customers/me/addresses/{addressId}/default", addressId)
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
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

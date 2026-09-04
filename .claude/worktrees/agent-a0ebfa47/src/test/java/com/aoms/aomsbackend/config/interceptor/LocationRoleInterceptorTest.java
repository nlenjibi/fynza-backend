package com.aoms.aomsbackend.config.interceptor;

import com.aoms.aomsbackend.auth.constant.SessionAttribute;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.auth.service.UserRoleAccessService;
import com.aoms.aomsbackend.common.annotation.RequiresRole;
import com.aoms.aomsbackend.common.exception.ForbiddenException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationRoleInterceptorTest {

    @Mock
    private UserRoleAccessService userRoleAccessService;

    private LocationRoleInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new LocationRoleInterceptor(userRoleAccessService);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void throwsForbiddenWhenSessionUserIdIsMissing() {
        MockHttpServletRequest request = requestWithOrganization();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> interceptor.preHandle(request, response, managerHandler()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void throwsForbiddenWhenOrganizationIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/protected");
        Objects.requireNonNull(request.getSession(true)).setAttribute(SessionAttribute.USER_ID.getKey(), UUID.randomUUID().toString());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> interceptor.preHandle(request, response, managerHandler()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void throwsForbiddenWhenAccessServiceRejectsRequest() {
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        MockHttpServletRequest request = requestWithUserAndOrganization(userId, organizationId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(userRoleAccessService.hasAccess(eq(userId), eq(organizationId), eq(UserRoleType.MANAGER))).thenReturn(false);

        assertThatThrownBy(() -> interceptor.preHandle(request, response, managerHandler()))
                .isInstanceOf(ForbiddenException.class);
        verify(userRoleAccessService).hasAccess(eq(userId), eq(organizationId), eq(UserRoleType.MANAGER));
    }

    @Test
    void allowsWhenAccessServiceApprovesRequest() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        MockHttpServletRequest request = requestWithUserAndOrganization(userId, organizationId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(userRoleAccessService.hasAccess(eq(userId), eq(organizationId), eq(UserRoleType.MANAGER))).thenReturn(true);

        boolean allowed = interceptor.preHandle(request, response, managerHandler());

        assertThat(allowed).isTrue();
        verify(userRoleAccessService).hasAccess(eq(userId), eq(organizationId), eq(UserRoleType.MANAGER));
    }

    @Test
    void allowsWhenHandlerHasNoRequiresRoleAnnotation() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/public");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, openHandler());

        assertThat(allowed).isTrue();
        verifyNoInteractions(userRoleAccessService);
    }

    private MockHttpServletRequest requestWithOrganization() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/locations/{buildingId}/manager");
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
                Map.of("buildingId", UUID.randomUUID().toString()));
        return request;
    }

    private MockHttpServletRequest requestWithUserAndOrganization(UUID userId, UUID organizationId) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/locations/{buildingId}/manager");
        Objects.requireNonNull(request.getSession(true)).setAttribute(SessionAttribute.USER_ID.getKey(), userId.toString());
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
                Map.of("buildingId", organizationId.toString()));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        return request;
    }

    private HandlerMethod managerHandler() throws NoSuchMethodException {
        Method method = TestController.class.getMethod("managerOnly");
        return new HandlerMethod(new TestController(), method);
    }

    private HandlerMethod openHandler() throws NoSuchMethodException {
        Method method = TestController.class.getMethod("open");
        return new HandlerMethod(new TestController(), method);
    }

    private static class TestController {

        @RequiresRole(UserRoleType.MANAGER)
        public void managerOnly() {
        }

        public void open() {
        }
    }
}


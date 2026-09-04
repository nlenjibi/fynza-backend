package com.aoms.aomsbackend.config.interceptor;

import com.aoms.aomsbackend.auth.service.UserRoleAccessService;
import com.aoms.aomsbackend.common.annotation.RequiresRole;
import com.aoms.aomsbackend.common.exception.ForbiddenException;
import com.aoms.aomsbackend.config.util.SessionUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.UUID;

/**
 * The type Location role interceptor.
 */
@Component
@RequiredArgsConstructor
public class LocationRoleInterceptor implements HandlerInterceptor {

    private final UserRoleAccessService userRoleAccessService;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequiresRole requiredRole = resolveAnnotation(handlerMethod);
        if (requiredRole == null) {
            return true;
        }

        UUID userId = SessionUtils.extractUserId();
        if (userId == null) {
            throw new ForbiddenException();
        }

        UUID buildingId = resolveBuildingId(request);

        if (!userRoleAccessService.hasAccess(userId, buildingId, requiredRole.value())) {
            throw new ForbiddenException();
        }

        return true;
    }

    private RequiresRole resolveAnnotation(HandlerMethod handlerMethod) {
        RequiresRole annotation = handlerMethod.getMethodAnnotation(RequiresRole.class);
        return annotation != null ? annotation : handlerMethod.getBeanType().getAnnotation(RequiresRole.class);
    }

    private UUID resolveBuildingId(HttpServletRequest request) {
        Object uriVariableMap = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (uriVariableMap instanceof Map<?, ?> pathVariables) {
            UUID buildingId = SessionUtils.parseUuid(pathVariables.get("buildingId"));
            if (buildingId != null) {
                return buildingId;
            }
            buildingId = SessionUtils.parseUuid(pathVariables.get("locationId"));
            if (buildingId != null) {
                return buildingId;
            }
            buildingId = SessionUtils.parseUuid(pathVariables.get("organizationId"));
            if (buildingId != null) {
                return buildingId;
            }
        }
        UUID fromBuildingHeader = SessionUtils.parseUuid(request.getHeader("X-Building-Id"));
        if (fromBuildingHeader != null) {
            return fromBuildingHeader;
        }
        return SessionUtils.parseUuid(request.getHeader("X-Organization-Id"));
    }

}

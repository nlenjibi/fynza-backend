package com.aoms.aomsbackend.config.interceptor;

import com.aoms.aomsbackend.auth.constant.SessionAttribute;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.auth.service.UserRoleAccessService;
import com.aoms.aomsbackend.common.annotation.RequiresRole;
import com.aoms.aomsbackend.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LocationRoleInterceptorIntegrationTest {

    @Mock
    private UserRoleAccessService userRoleAccessService;

    @Test
    void returnsForbiddenWhenSessionHasNoUserId() throws Exception {
        MockMvc mockMvc = buildMockMvc();
        UUID buildingId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/test/locations/{buildingId}/manager", buildingId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsForbiddenWhenAccessServiceRejectsRequest() throws Exception {
        MockMvc mockMvc = buildMockMvc();
        UUID userId = UUID.randomUUID();
        UUID buildingId = UUID.randomUUID();

        when(userRoleAccessService.hasAccess(eq(userId), eq(buildingId), eq(UserRoleType.MANAGER))).thenReturn(false);

        mockMvc.perform(get("/api/v1/test/locations/{buildingId}/manager", buildingId)
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsWhenAccessServiceApprovesRequest() throws Exception {
        MockMvc mockMvc = buildMockMvc();
        UUID userId = UUID.randomUUID();
        UUID buildingId = UUID.randomUUID();

        when(userRoleAccessService.hasAccess(eq(userId), eq(buildingId), eq(UserRoleType.MANAGER))).thenReturn(true);

        mockMvc.perform(get("/api/v1/test/locations/{buildingId}/manager", buildingId)
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), userId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    private MockMvc buildMockMvc() {
        return MockMvcBuilders.standaloneSetup(new TestController())
                .addInterceptors(new LocationRoleInterceptor(userRoleAccessService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @RestController
    @RequestMapping("/api/v1/test")
    static class TestController {

        @RequiresRole(UserRoleType.MANAGER)
        @GetMapping("/locations/{buildingId}/manager")
        public String managerOnly(@PathVariable UUID buildingId) {
            return "ok";
        }
    }
}



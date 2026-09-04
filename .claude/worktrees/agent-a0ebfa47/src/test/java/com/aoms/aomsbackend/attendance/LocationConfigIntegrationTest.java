package com.aoms.aomsbackend.attendance;

import com.aoms.aomsbackend.attendance.dto.LocationConfigUpdateRequest;
import com.aoms.aomsbackend.attendance.entity.LocationConfig;
import com.aoms.aomsbackend.attendance.entity.SeatVisibilityMode;
import com.aoms.aomsbackend.attendance.repository.LocationConfigRepository;
import com.aoms.aomsbackend.auth.constant.SessionAttribute;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.auth.service.UserRoleAccessService;
import com.aoms.aomsbackend.config.TestSecurityConfig;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.context.annotation.Import;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(TestSecurityConfig.class)
class LocationConfigIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private LocationConfigRepository repository;

    @MockitoBean
    private UserRoleAccessService userRoleAccessService;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private final UUID hrUserId = UUID.randomUUID();
    private final UUID managerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void getConfig_existingLocation_returnsConfig() throws Exception {
        LocationConfig config = seedConfig();
        UUID locationId = config.getBuildingId();

        when(userRoleAccessService.hasAccess(eq(managerId), eq(locationId), eq(UserRoleType.EMPLOYEE))).thenReturn(true);

        mockMvc.perform(get("/api/v1/locations/{locationId}/config", locationId)
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), managerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.minPresenceDurationMinutes").value(360))
                .andExpect(jsonPath("$.data.buildingId").value(locationId.toString()));
    }

    @Test
    void updateConfig_persistsNewThreshold() throws Exception {
        LocationConfig config = seedConfig();
        UUID locationId = config.getBuildingId();

        when(userRoleAccessService.hasAccess(eq(hrUserId), eq(locationId), eq(UserRoleType.HR))).thenReturn(true);

        LocationConfigUpdateRequest request = new LocationConfigUpdateRequest();
        request.setMinPresenceDurationMinutes(480);

        mockMvc.perform(patch("/api/v1/locations/{locationId}/config", locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request))
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), hrUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.minPresenceDurationMinutes").value(480));

        LocationConfig updated = repository.findByBuildingId(locationId).orElseThrow();
        assertThat(updated.getMinPresenceDurationMinutes()).isEqualTo(480);
    }

    @Test
    void updateConfig_reflectedOnNextGet() throws Exception {
        LocationConfig config = seedConfig();
        UUID locationId = config.getBuildingId();

        when(userRoleAccessService.hasAccess(eq(hrUserId), eq(locationId), eq(UserRoleType.HR))).thenReturn(true);
        when(userRoleAccessService.hasAccess(eq(managerId), eq(locationId), eq(UserRoleType.EMPLOYEE))).thenReturn(true);

        LocationConfigUpdateRequest request = new LocationConfigUpdateRequest();
        request.setMinPresenceDurationMinutes(300);

        mockMvc.perform(patch("/api/v1/locations/{locationId}/config", locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request))
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), hrUserId.toString()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/locations/{locationId}/config", locationId)
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), managerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.minPresenceDurationMinutes").value(300));
    }

    @Test
    void updateConfig_workStartTime_persistsChange() throws Exception {
        LocationConfig config = seedConfig();
        UUID locationId = config.getBuildingId();

        when(userRoleAccessService.hasAccess(eq(hrUserId), eq(locationId), eq(UserRoleType.HR))).thenReturn(true);

        LocationConfigUpdateRequest request = new LocationConfigUpdateRequest();
        request.setWorkStartTime(LocalTime.of(8, 30));

        mockMvc.perform(patch("/api/v1/locations/{locationId}/config", locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request))
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), hrUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workStartTime").value("08:30:00"));

        LocationConfig updated = repository.findByBuildingId(locationId).orElseThrow();
        assertThat(updated.getWorkStartTime()).isEqualTo(LocalTime.of(8, 30));
        assertThat(updated.getMinPresenceDurationMinutes()).isEqualTo(360);
    }

    @Test
    void updateConfig_multipleFields_onlyChangesSupplied() throws Exception {
        LocationConfig config = seedConfig();
        UUID locationId = config.getBuildingId();

        when(userRoleAccessService.hasAccess(eq(hrUserId), eq(locationId), eq(UserRoleType.HR))).thenReturn(true);

        LocationConfigUpdateRequest request = new LocationConfigUpdateRequest();
        request.setLatenessThresholdMinutes(20);
        request.setHotDeskBookingWindowDays(14);

        mockMvc.perform(patch("/api/v1/locations/{locationId}/config", locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request))
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), hrUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.latenessThresholdMinutes").value(20))
                .andExpect(jsonPath("$.data.hotDeskBookingWindowDays").value(14));

        LocationConfig updated = repository.findByBuildingId(locationId).orElseThrow();
        assertThat(updated.getLatenessThresholdMinutes()).isEqualTo(20);
        assertThat(updated.getHotDeskBookingWindowDays()).isEqualTo(14);
        assertThat(updated.getMinPresenceDurationMinutes()).isEqualTo(360);
    }

    @Test
    void updateConfig_singleField_preservesAllOtherFields() throws Exception {
        LocationConfig config = seedConfig();
        UUID locationId = config.getBuildingId();

        when(userRoleAccessService.hasAccess(eq(hrUserId), eq(locationId), eq(UserRoleType.HR))).thenReturn(true);

        LocationConfigUpdateRequest request = new LocationConfigUpdateRequest();
        request.setLatenessThresholdMinutes(20);

        mockMvc.perform(patch("/api/v1/locations/{locationId}/config", locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request))
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), hrUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.latenessThresholdMinutes").value(20));

        LocationConfig updated = repository.findByBuildingId(locationId).orElseThrow();
        assertThat(updated.getLatenessThresholdMinutes()).isEqualTo(20);
        assertThat(updated.getWorkStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(updated.getMinPresenceDurationMinutes()).isEqualTo(360);
        assertThat(updated.getNoShowReleaseTime()).isNull();
        assertThat(updated.getHotDeskBookingWindowDays()).isEqualTo(7);
        assertThat(updated.getBookingCancellationCutoffHours()).isEqualTo(2);
        assertThat(updated.getSessionGapThresholdHours()).isEqualTo(4);
    }

    @Test
    void updateConfig_emptyBody_returns400() throws Exception {
        LocationConfig config = seedConfig();
        UUID locationId = config.getBuildingId();

        when(userRoleAccessService.hasAccess(eq(hrUserId), eq(locationId), eq(UserRoleType.HR))).thenReturn(true);

        mockMvc.perform(patch("/api/v1/locations/{locationId}/config", locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), hrUserId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getConfig_nonExistentLocation_returns404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(userRoleAccessService.hasAccess(eq(managerId), eq(unknownId), eq(UserRoleType.EMPLOYEE))).thenReturn(true);

        mockMvc.perform(get("/api/v1/locations/{locationId}/config", unknownId)
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), managerId.toString()))
                .andExpect(status().isNotFound());
    }

    private LocationConfig seedConfig() {
        LocationConfig config = new LocationConfig();
        config.setBuildingId(UUID.randomUUID());
        config.setWorkStartTime(LocalTime.of(9, 0));
        config.setLatenessThresholdMinutes(15);
        config.setMinPresenceDurationMinutes(360);
        config.setHotDeskBookingWindowDays(7);
        config.setBookingCancellationCutoffHours(2);
        config.setSeatVisibilityMode(SeatVisibilityMode.FULL);
        config.setSessionGapThresholdHours(4);
        config.setUpdatedAt(OffsetDateTime.now());
        return repository.save(config);
    }
}

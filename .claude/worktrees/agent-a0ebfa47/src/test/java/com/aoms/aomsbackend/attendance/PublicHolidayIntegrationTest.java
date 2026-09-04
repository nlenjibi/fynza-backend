package com.aoms.aomsbackend.attendance;

import com.aoms.aomsbackend.attendance.dto.PublicHolidayCreateRequest;
import com.aoms.aomsbackend.attendance.dto.PublicHolidayUpdateRequest;
import com.aoms.aomsbackend.attendance.entity.OfficeBuilding;
import com.aoms.aomsbackend.attendance.entity.PublicHoliday;
import com.aoms.aomsbackend.attendance.repository.OfficeBuildingRepository;
import com.aoms.aomsbackend.attendance.repository.PublicHolidayRepository;
import com.aoms.aomsbackend.attendance.service.AttendancePass2Service;
import com.aoms.aomsbackend.auth.constant.SessionAttribute;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.auth.service.UserRoleAccessService;
import com.aoms.aomsbackend.config.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the public holidays API.
 * Uses an in-memory H2 database (test profile) and mocks all external dependencies
 * (AWS SDK clients, SNS publisher, JWT decoder) to avoid real infrastructure calls.
 * {@link AttendancePass2Service} is mocked so the {@code HolidayRestampListener} does not
 * attempt a real attendance overlay during the test.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class PublicHolidayIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private PublicHolidayRepository repository;

    @Autowired
    private OfficeBuildingRepository officeBuildingRepository;

    @MockitoBean
    private UserRoleAccessService userRoleAccessService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AttendancePass2Service pass2Service;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final UUID hrUserId = UUID.randomUUID();
    private final UUID superAdminId = UUID.randomUUID();
    private final UUID locationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
        officeBuildingRepository.deleteAll();
    }

    // ── POST ──────────────────────────────────────────────────────────────────

    /**
     * Posting a valid future holiday as HR results in 201 with the full response body.
     */
    @Test
    void post_validFutureHoliday_asHR_returns201() throws Exception {
        givenHrAccess();

        PublicHolidayCreateRequest request = createRequest(LocalDate.now().plusDays(10), "Test Holiday");

        mockMvc.perform(post("/api/v1/locations/{locationId}/public-holidays", locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), hrUserId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Test Holiday"))
                .andExpect(jsonPath("$.data.locationId").value(locationId.toString()))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    /**
     * Posting a duplicate (same location + date) results in 409 with HOLIDAY_ALREADY_EXISTS code.md.
     */
    @Test
    void post_duplicateDate_returns409() throws Exception {
        givenHrAccess();
        LocalDate date = LocalDate.now().plusDays(5);
        seedHoliday(date);

        PublicHolidayCreateRequest request = createRequest(date, "Duplicate");

        mockMvc.perform(post("/api/v1/locations/{locationId}/public-holidays", locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), hrUserId.toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code.md").value("HOLIDAY_ALREADY_EXISTS"));
    }

    /**
     * HR posting a past-date holiday is rejected with 400.
     */
    @Test
    void post_pastDate_asHR_returns400() throws Exception {
        givenHrAccess();

        PublicHolidayCreateRequest request = createRequest(LocalDate.now().minusDays(1), "Past Holiday");

        mockMvc.perform(post("/api/v1/locations/{locationId}/public-holidays", locationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), hrUserId.toString()))
                .andExpect(status().isBadRequest());
    }

    // ── GET ───────────────────────────────────────────────────────────────────

    /**
     * GET without year filter returns all holidays sorted ASC.
     */
    @Test
    void get_withoutYear_returnsAllHolidaysSortedAsc() throws Exception {
        givenEmployeeAccess(hrUserId);
        seedHoliday(LocalDate.of(2027, 3, 1));
        seedHoliday(LocalDate.of(2027, 1, 1));

        mockMvc.perform(get("/api/v1/locations/{locationId}/public-holidays", locationId)
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), hrUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].holidayDate").value("2027-01-01"))
                .andExpect(jsonPath("$.data[1].holidayDate").value("2027-03-01"));
    }

    /**
     * GET with ?year filter returns only holidays in that year.
     */
    @Test
    void get_withYearFilter_returnsOnlyThatYearHolidays() throws Exception {
        givenEmployeeAccess(hrUserId);
        seedHoliday(LocalDate.of(2027, 6, 1));
        seedHoliday(LocalDate.of(2028, 1, 1));

        mockMvc.perform(get("/api/v1/locations/{locationId}/public-holidays", locationId)
                        .param("year", "2027")
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), hrUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].holidayDate").value("2027-06-01"));
    }

    // ── PUT ───────────────────────────────────────────────────────────────────

    /**
     * Updating a future holiday's name succeeds and returns the updated record.
     */
    @Test
    void put_futureHoliday_updatesName() throws Exception {
        givenHrAccess();
        PublicHoliday holiday = seedHoliday(LocalDate.now().plusDays(20));

        PublicHolidayUpdateRequest request = new PublicHolidayUpdateRequest();
        request.setName("Renamed Holiday");

        mockMvc.perform(put("/api/v1/locations/{locationId}/public-holidays/{holidayId}",
                        locationId, holiday.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), hrUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Renamed Holiday"));
    }

    /**
     * Updating a past holiday returns 400 with PAST_HOLIDAY_IMMUTABLE code.md.
     */
    @Test
    void put_pastHoliday_returns400WithCode() throws Exception {
        givenHrAccess();
        PublicHoliday holiday = seedHoliday(LocalDate.now().minusDays(5));

        PublicHolidayUpdateRequest request = new PublicHolidayUpdateRequest();
        request.setName("New Name");

        mockMvc.perform(put("/api/v1/locations/{locationId}/public-holidays/{holidayId}",
                        locationId, holiday.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), hrUserId.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code.md").value("PAST_HOLIDAY_IMMUTABLE"));
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    /**
     * HR deleting a future holiday returns 200 with restampQueued=false.
     */
    @Test
    void delete_futureHoliday_asHR_returnsRestampFalse() throws Exception {
        givenHrAccess();
        PublicHoliday holiday = seedHoliday(LocalDate.now().plusDays(30));

        mockMvc.perform(delete("/api/v1/locations/{locationId}/public-holidays/{holidayId}",
                        locationId, holiday.getId())
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), hrUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.restampQueued").value(false));
    }

    /**
     * SUPER_ADMIN deleting a past holiday returns 200 with restampQueued=true
     * and triggers the Pass 2 overlay via the HolidayRestampListener (AFTER_COMMIT).
     */
    @Test
    void delete_pastHoliday_asSuperAdmin_returnsRestampTrueAndTriggersOverlay() throws Exception {
        givenSuperAdminAccess();
        OfficeBuilding building = seedOfficeBuilding();
        LocalDate pastDate = LocalDate.now().minusDays(15);
        PublicHoliday holiday = seedHoliday(pastDate);

        mockMvc.perform(delete("/api/v1/locations/{locationId}/public-holidays/{holidayId}",
                        locationId, holiday.getId())
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), superAdminId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.restampQueued").value(true));

        verify(pass2Service).overlay(locationId, building.getOfficeId(), pastDate);
    }

    /**
     * HR attempting to delete a past holiday is rejected with 403.
     */
    @Test
    void delete_pastHoliday_asHR_returns403() throws Exception {
        givenHrAccess();
        PublicHoliday holiday = seedHoliday(LocalDate.now().minusDays(5));

        mockMvc.perform(delete("/api/v1/locations/{locationId}/public-holidays/{holidayId}",
                        locationId, holiday.getId())
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), hrUserId.toString()))
                .andExpect(status().isForbidden());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void givenHrAccess() {
        when(userRoleAccessService.hasAccess(hrUserId, locationId, UserRoleType.EMPLOYEE)).thenReturn(true);
        when(userRoleAccessService.hasAccess(hrUserId, locationId, UserRoleType.HR)).thenReturn(true);
        when(userRoleAccessService.hasAccess(hrUserId, locationId, UserRoleType.SUPER_ADMIN)).thenReturn(false);
    }

    private void givenSuperAdminAccess() {
        when(userRoleAccessService.hasAccess(superAdminId, locationId, UserRoleType.EMPLOYEE)).thenReturn(true);
        when(userRoleAccessService.hasAccess(superAdminId, locationId, UserRoleType.HR)).thenReturn(true);
        when(userRoleAccessService.hasAccess(superAdminId, locationId, UserRoleType.SUPER_ADMIN)).thenReturn(true);
    }

    private void givenEmployeeAccess(UUID userId) {
        when(userRoleAccessService.hasAccess(userId, locationId, UserRoleType.EMPLOYEE)).thenReturn(true);
        when(userRoleAccessService.hasAccess(userId, locationId, UserRoleType.HR)).thenReturn(false);
        when(userRoleAccessService.hasAccess(userId, locationId, UserRoleType.SUPER_ADMIN)).thenReturn(false);
    }

    private PublicHoliday seedHoliday(LocalDate date) {
        PublicHoliday holiday = new PublicHoliday();
        holiday.setBuildingId(locationId);
        holiday.setHolidayDate(date);
        holiday.setName("Seeded Holiday " + date);
        return repository.save(holiday);
    }

    private PublicHolidayCreateRequest createRequest(LocalDate date, String name) {
        PublicHolidayCreateRequest req = new PublicHolidayCreateRequest();
        req.setHolidayDate(date);
        req.setName(name);
        return req;
    }

    /**
     * Seeds an OfficeBuilding with {@code id=locationId} so the restamp flow can resolve officeId.
     */
    private OfficeBuilding seedOfficeBuilding() {
        OfficeBuilding building = OfficeBuilding.builder()
                .id(locationId)
                .officeId(UUID.randomUUID())
                .buildingName("Test Building")
                .active(true)
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();
        return officeBuildingRepository.save(building);
    }
}

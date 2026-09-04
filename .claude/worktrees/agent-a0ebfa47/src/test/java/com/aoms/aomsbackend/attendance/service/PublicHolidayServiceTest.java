package com.aoms.aomsbackend.attendance.service;

import com.aoms.aomsbackend.attendance.dto.DeleteHolidayResponse;
import com.aoms.aomsbackend.attendance.dto.PublicHolidayCreateRequest;
import com.aoms.aomsbackend.attendance.dto.PublicHolidayResponse;
import com.aoms.aomsbackend.attendance.dto.PublicHolidayUpdateRequest;
import com.aoms.aomsbackend.attendance.entity.OfficeBuilding;
import com.aoms.aomsbackend.attendance.entity.PublicHoliday;
import com.aoms.aomsbackend.attendance.event.HolidayDeletedEvent;
import com.aoms.aomsbackend.attendance.repository.OfficeBuildingRepository;
import com.aoms.aomsbackend.attendance.repository.PublicHolidayRepository;
import com.aoms.aomsbackend.attendance.service.impl.PublicHolidayServiceImpl;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.common.exception.BadRequestException;
import com.aoms.aomsbackend.common.exception.ConflictException;
import com.aoms.aomsbackend.common.exception.ForbiddenException;
import com.aoms.aomsbackend.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PublicHolidayServiceImpl}.
 * External dependencies (repository, SNS publisher, event publisher) are mocked via Mockito.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PublicHolidayServiceTest {

    @Mock
    private PublicHolidayRepository holidayRepository;

    @Mock
    private OfficeBuildingRepository officeBuildingRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PublicHolidayServiceImpl service;

    private static final UUID LOCATION_ID = UUID.randomUUID();
    private static final UUID HOLIDAY_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final UUID OFFICE_ID = UUID.randomUUID();
    private static final LocalDate FUTURE_DATE = LocalDate.now().plusDays(10);
    private static final LocalDate PAST_DATE = LocalDate.now().minusDays(10);

    @BeforeEach
    void setUp() {
        when(holidayRepository.save(any(PublicHoliday.class))).thenAnswer(inv -> {
            PublicHoliday h = inv.getArgument(0);
            if (h.getId() == null) h.setId(UUID.randomUUID());
            return h;
        });
    }

    // ── CREATE ─────────────────────────────────────────────────────────────────

    /**
     * HR users can create future public holidays successfully.
     */
    @Test
    void create_futureDate_asHR_savesAndReturnsResponse() {
        when(holidayRepository.findByBuildingIdAndHolidayDate(LOCATION_ID, FUTURE_DATE))
                .thenReturn(Optional.empty());

        PublicHolidayCreateRequest request = new PublicHolidayCreateRequest();
        request.setHolidayDate(FUTURE_DATE);
        request.setName("Test Holiday");

        PublicHolidayResponse response = service.create(LOCATION_ID, request, ACTOR_ID, UserRoleType.HR);

        assertThat(response.getLocationId()).isEqualTo(LOCATION_ID);
        assertThat(response.getHolidayDate()).isEqualTo(FUTURE_DATE);
        assertThat(response.getName()).isEqualTo("Test Holiday");
        verify(holidayRepository).save(any(PublicHoliday.class));
    }

    /**
     * HR users are blocked from creating holidays with a past date.
     */
    @Test
    void create_pastDate_asHR_throws400() {
        PublicHolidayCreateRequest request = new PublicHolidayCreateRequest();
        request.setHolidayDate(PAST_DATE);
        request.setName("Old Holiday");

        assertThatThrownBy(() -> service.create(LOCATION_ID, request, ACTOR_ID, UserRoleType.HR))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("past");

        verify(holidayRepository, never()).save(any());
    }

    /**
     * SUPER_ADMIN users may create holidays on past dates.
     */
    @Test
    void create_pastDate_asSuperAdmin_savesSuccessfully() {
        when(holidayRepository.findByBuildingIdAndHolidayDate(LOCATION_ID, PAST_DATE))
                .thenReturn(Optional.empty());

        PublicHolidayCreateRequest request = new PublicHolidayCreateRequest();
        request.setHolidayDate(PAST_DATE);
        request.setName("Retroactive Holiday");

        PublicHolidayResponse response = service.create(LOCATION_ID, request, ACTOR_ID, UserRoleType.SUPER_ADMIN);

        assertThat(response.getHolidayDate()).isEqualTo(PAST_DATE);
        verify(holidayRepository).save(any(PublicHoliday.class));
    }

    /**
     * Duplicate (locationId, date) combination returns 409 ConflictException.
     */
    @Test
    void create_duplicateDate_throws409() {
        when(holidayRepository.findByBuildingIdAndHolidayDate(LOCATION_ID, FUTURE_DATE))
                .thenReturn(Optional.of(buildHoliday(FUTURE_DATE)));

        PublicHolidayCreateRequest request = new PublicHolidayCreateRequest();
        request.setHolidayDate(FUTURE_DATE);
        request.setName("Duplicate");

        assertThatThrownBy(() -> service.create(LOCATION_ID, request, ACTOR_ID, UserRoleType.HR))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("code.md", "HOLIDAY_ALREADY_EXISTS");

        verify(holidayRepository, never()).save(any());
    }

    // ── LIST ───────────────────────────────────────────────────────────────────

    /**
     * Without a year filter, all holidays for the location are returned.
     */
    @Test
    void list_noYear_returnsAllHolidays() {
        when(holidayRepository.findByBuildingIdOrderByHolidayDateAsc(LOCATION_ID))
                .thenReturn(List.of(buildHoliday(FUTURE_DATE), buildHoliday(FUTURE_DATE.plusDays(5))));

        List<PublicHolidayResponse> result = service.list(LOCATION_ID, null);

        assertThat(result).hasSize(2);
        verify(holidayRepository).findByBuildingIdOrderByHolidayDateAsc(LOCATION_ID);
        verify(holidayRepository, never()).findByBuildingIdAndHolidayDateBetweenOrderByHolidayDateAsc(any(), any(), any());
    }

    /**
     * With a year filter, only holidays in that year are returned.
     */
    @Test
    void list_withYear_returnsFilteredHolidays() {
        int year = 2026;
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        when(holidayRepository.findByBuildingIdAndHolidayDateBetweenOrderByHolidayDateAsc(LOCATION_ID, start, end))
                .thenReturn(List.of(buildHoliday(LocalDate.of(year, 3, 6))));

        List<PublicHolidayResponse> result = service.list(LOCATION_ID, year);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getHolidayDate().getYear()).isEqualTo(year);
    }

    // ── UPDATE ─────────────────────────────────────────────────────────────────

    /**
     * Updating a future holiday's name succeeds and publishes an audit event.
     */
    @Test
    void update_futureHoliday_updatesNameSuccessfully() {
        PublicHoliday existing = buildHoliday(FUTURE_DATE);
        when(holidayRepository.findById(HOLIDAY_ID)).thenReturn(Optional.of(existing));
        when(holidayRepository.save(any())).thenReturn(existing);

        PublicHolidayUpdateRequest request = new PublicHolidayUpdateRequest();
        request.setName("New Name");

        PublicHolidayResponse response = service.update(LOCATION_ID, HOLIDAY_ID, request);

        assertThat(response.getName()).isEqualTo("New Name");
    }

    /**
     * PUT with both fields null is rejected with 400 before any DB access.
     */
    @Test
    void update_bothFieldsNull_throwsBadRequest() {
        PublicHolidayUpdateRequest updateRequest = new PublicHolidayUpdateRequest();

        assertThatThrownBy(() -> service.update(LOCATION_ID, HOLIDAY_ID, updateRequest))
                .isInstanceOf(BadRequestException.class);

        verify(holidayRepository, never()).findById(any());
    }

    /**
     * Attempting to update a holiday with a past date is rejected with 400 PAST_HOLIDAY_IMMUTABLE.
     */
    @Test
    void update_pastHoliday_throws400WithCode() {
        PublicHoliday existing = buildHoliday(PAST_DATE);
        when(holidayRepository.findById(HOLIDAY_ID)).thenReturn(Optional.of(existing));

        PublicHolidayUpdateRequest request = new PublicHolidayUpdateRequest();
        request.setName("New Name");

        assertThatThrownBy(() -> service.update(LOCATION_ID, HOLIDAY_ID, request))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("code.md", "PAST_HOLIDAY_IMMUTABLE");

        verify(holidayRepository, never()).save(any());
    }

    /**
     * Updating a non-existent holiday returns 404.
     */
    @Test
    void update_notFound_throws404() {
        when(holidayRepository.findById(HOLIDAY_ID)).thenReturn(Optional.empty());

        PublicHolidayUpdateRequest request = new PublicHolidayUpdateRequest();
        request.setName("anything");

        assertThatThrownBy(() -> service.update(LOCATION_ID, HOLIDAY_ID, request))
                .isInstanceOf(NotFoundException.class);
    }

    /**
     * Updating the date to one that already has a holiday on it returns 409.
     */
    @Test
    void update_dateConflict_throws409() {
        LocalDate conflictDate = FUTURE_DATE.plusDays(3);
        PublicHoliday existing = buildHoliday(FUTURE_DATE);
        when(holidayRepository.findById(HOLIDAY_ID)).thenReturn(Optional.of(existing));
        when(holidayRepository.findByBuildingIdAndHolidayDate(LOCATION_ID, conflictDate))
                .thenReturn(Optional.of(buildHoliday(conflictDate)));

        PublicHolidayUpdateRequest request = new PublicHolidayUpdateRequest();
        request.setHolidayDate(conflictDate);

        assertThatThrownBy(() -> service.update(LOCATION_ID, HOLIDAY_ID, request))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("code.md", "HOLIDAY_ALREADY_EXISTS");
    }

    // ── DELETE ─────────────────────────────────────────────────────────────────

    /**
     * HR deleting a future holiday succeeds with restampQueued=false.
     */
    @Test
    void delete_futureHoliday_asHR_deletesWithoutRestamp() {
        PublicHoliday existing = buildHoliday(FUTURE_DATE);
        when(holidayRepository.findById(HOLIDAY_ID)).thenReturn(Optional.of(existing));

        DeleteHolidayResponse response = service.delete(LOCATION_ID, HOLIDAY_ID, UserRoleType.HR);

        assertThat(response.isRestampQueued()).isFalse();
        verify(holidayRepository).delete(existing);
        verify(eventPublisher, never()).publishEvent(any());
    }

    /**
     * SUPER_ADMIN deleting a past holiday hard-deletes it and publishes a HolidayDeletedEvent.
     */
    @Test
    void delete_pastHoliday_asSuperAdmin_deletesAndQueuesRestamp() {
        PublicHoliday existing = buildHoliday(PAST_DATE);
        OfficeBuilding officeBuilding = OfficeBuilding.builder().id(LOCATION_ID).officeId(OFFICE_ID).build();
        when(holidayRepository.findById(HOLIDAY_ID)).thenReturn(Optional.of(existing));
        when(officeBuildingRepository.findById(LOCATION_ID)).thenReturn(Optional.of(officeBuilding));

        DeleteHolidayResponse response = service.delete(LOCATION_ID, HOLIDAY_ID, UserRoleType.SUPER_ADMIN);

        assertThat(response.isRestampQueued()).isTrue();
        verify(holidayRepository).delete(existing);
        verify(eventPublisher).publishEvent(new HolidayDeletedEvent(LOCATION_ID, OFFICE_ID, PAST_DATE));
    }

    /**
     * HR attempting to delete a past holiday receives 403 Forbidden.
     */
    @Test
    void delete_pastHoliday_asHR_throws403() {
        PublicHoliday existing = buildHoliday(PAST_DATE);
        when(holidayRepository.findById(HOLIDAY_ID)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.delete(LOCATION_ID, HOLIDAY_ID, UserRoleType.HR))
                .isInstanceOf(ForbiddenException.class);

        verify(holidayRepository, never()).delete(any());
    }

    /**
     * Deleting a non-existent holiday returns 404.
     */
    @Test
    void delete_notFound_throws404() {
        when(holidayRepository.findById(HOLIDAY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(LOCATION_ID, HOLIDAY_ID, UserRoleType.HR))
                .isInstanceOf(NotFoundException.class);
    }

    /**
     * When no OfficeBuilding record is found, the delete throws IllegalStateException
     * (data integrity issue) and the holiday is not deleted.
     */
    @Test
    void delete_noOfficeBuildingFound_throwsIllegalStateException() {
        PublicHoliday existing = buildHoliday(PAST_DATE);
        when(holidayRepository.findById(HOLIDAY_ID)).thenReturn(Optional.of(existing));
        when(officeBuildingRepository.findById(LOCATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(LOCATION_ID, HOLIDAY_ID, UserRoleType.SUPER_ADMIN))
                .isInstanceOf(IllegalStateException.class);

        verify(holidayRepository, never()).delete(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private PublicHoliday buildHoliday(LocalDate date) {
        PublicHoliday h = new PublicHoliday();
        h.setId(HOLIDAY_ID);
        h.setBuildingId(LOCATION_ID);
        h.setHolidayDate(date);
        h.setName("Test Holiday");
        h.setCreatedBy(ACTOR_ID);
        return h;
    }
}

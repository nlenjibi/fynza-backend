package com.aoms.aomsbackend.seating.service;

import com.aoms.aomsbackend.attendance.entity.Employee;
import com.aoms.aomsbackend.attendance.entity.LocationConfig;
import com.aoms.aomsbackend.seating.entity.SeatBookingStatus;
import com.aoms.aomsbackend.attendance.entity.SeatVisibilityMode;
import com.aoms.aomsbackend.attendance.repository.EmployeeRepository;
import com.aoms.aomsbackend.attendance.repository.LocationConfigRepository;
import com.aoms.aomsbackend.auth.entity.User;
import com.aoms.aomsbackend.auth.repository.UserRepository;
import com.aoms.aomsbackend.common.exception.NotFoundException;
import com.aoms.aomsbackend.seating.dto.FloorPlanResponse;
import com.aoms.aomsbackend.seating.dto.FloorPlanSeatResponse;
import com.aoms.aomsbackend.seating.entity.Floor;
import com.aoms.aomsbackend.seating.entity.Seat;
import com.aoms.aomsbackend.seating.entity.SeatBooking;
import com.aoms.aomsbackend.seating.entity.SeatStatus;
import com.aoms.aomsbackend.seating.entity.SeatType;
import com.aoms.aomsbackend.seating.repository.FloorRepository;
import com.aoms.aomsbackend.seating.repository.SeatBookingRepository;
import com.aoms.aomsbackend.seating.repository.SeatRepository;
import com.aoms.aomsbackend.seating.service.impl.FloorPlanServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FloorPlanServiceImplTest {

    @Mock
    private FloorRepository floorRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private SeatBookingRepository seatBookingRepository;

    @Mock
    private LocationConfigRepository locationConfigRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FloorPlanServiceImpl service;

    private UUID buildingId;
    private UUID floorId;
    private UUID seatId;
    private UUID requestingUserId;
    private UUID occupantId;
    private Employee occupantEmployee;

    @BeforeEach
    void setUp() {
        buildingId = UUID.randomUUID();
        floorId = UUID.randomUUID();
        seatId = UUID.randomUUID();
        requestingUserId = UUID.randomUUID();
        occupantId = UUID.randomUUID();

        occupantEmployee = Employee.builder()
                .id(occupantId)
                .firstName("Jane")
                .lastName("Doe")
                .department("Engineering")
                .active(true)
                .employmentStartDate(LocalDate.of(2020, 1, 1))
                .build();
    }

    private LocationConfig configWith(SeatVisibilityMode mode) {
        LocationConfig config = new LocationConfig();
        config.setSeatVisibilityMode(mode);
        return config;
    }

    private Floor floor() {
        return Floor.builder()
                .id(floorId)
                .buildingId(buildingId)
                .name("Ground Floor")
                .floorNumber(0)
                .active(true)
                .build();
    }

    private Seat fixedSeat(UUID assignedEmployeeId) {
        return Seat.builder()
                .id(seatId).floorId(floorId).seatNumber("A1")
                .seatType(SeatType.PERMANENT).status(SeatStatus.AVAILABLE)
                .assignedEmployeeId(assignedEmployeeId)
                .xPosition(1.0f).yPosition(2.0f)
                .build();
    }

    private Seat hotDeskSeat() {
        return Seat.builder()
                .id(seatId).floorId(floorId).seatNumber("H1")
                .seatType(SeatType.HOT_DESK).status(SeatStatus.AVAILABLE)
                .xPosition(3.0f).yPosition(4.0f)
                .build();
    }

    private SeatBooking bookingFor(UUID seatId, UUID userId) {
        return SeatBooking.builder()
                .id(UUID.randomUUID()).seatId(seatId).userId(userId)
                .buildingId(buildingId).bookingDate(LocalDate.now()).status(SeatBookingStatus.CONFIRMED)
                .build();
    }

    private void stubFloorAndBookings(Seat seat, List<SeatBooking> bookings) {
        when(floorRepository.findByBuildingIdAndActiveTrueAndDeletedAtIsNullOrderByFloorNumber(buildingId)).thenReturn(List.of(floor()));
        when(seatBookingRepository.findByBuildingIdAndBookingDateAndStatus(eq(buildingId), any(LocalDate.class), eq(SeatBookingStatus.CONFIRMED)))
                .thenReturn(bookings);
        when(seatRepository.findByFloorIdAndDeletedAtIsNull(floorId)).thenReturn(List.of(seat));
    }

    @Test
    void getFloorPlan_fullMode_returnsFullOccupantInfo() {
        when(locationConfigRepository.findByBuildingId(buildingId)).thenReturn(Optional.of(configWith(SeatVisibilityMode.FULL)));
        stubFloorAndBookings(fixedSeat(occupantId), List.of());
        when(employeeRepository.findAllById(any())).thenReturn(List.of(occupantEmployee));

        FloorPlanResponse result = service.getFloorPlan(buildingId, requestingUserId);

        FloorPlanSeatResponse seat = result.getFloors().getFirst().getSeats().getFirst();
        assertThat(seat.isOccupied()).isTrue();
        assertThat(seat.getOccupantInfo()).isNotNull();
        assertThat(seat.getOccupantInfo().getName()).isEqualTo("Jane Doe");
        assertThat(seat.getOccupantInfo().getDepartment()).isEqualTo("Engineering");
        assertThat(result.getSeatVisibilityMode()).isEqualTo(SeatVisibilityMode.FULL);
    }

    @Test
    void getFloorPlan_teamOnlyMode_sameDepartment_returnsOccupantInfo() {
        when(locationConfigRepository.findByBuildingId(buildingId)).thenReturn(Optional.of(configWith(SeatVisibilityMode.TEAM_ONLY)));
        stubFloorAndBookings(fixedSeat(occupantId), List.of());

        User requester = User.builder().id(requestingUserId).ssoUserId("sso-req").firstName("Bob").lastName("Smith").email("bob@test.com").build();
        Employee requesterEmployee = Employee.builder()
                .id(UUID.randomUUID()).ssoUserId("sso-req").firstName("Bob").lastName("Smith")
                .department("Engineering").active(true).employmentStartDate(LocalDate.of(2021, 1, 1)).build();
        when(userRepository.findById(requestingUserId)).thenReturn(Optional.of(requester));
        when(employeeRepository.findBySsoUserId("sso-req")).thenReturn(Optional.of(requesterEmployee));
        when(employeeRepository.findAllById(any())).thenReturn(List.of(occupantEmployee));

        FloorPlanResponse result = service.getFloorPlan(buildingId, requestingUserId);

        FloorPlanSeatResponse seat = result.getFloors().getFirst().getSeats().getFirst();
        assertThat(seat.isOccupied()).isTrue();
        assertThat(seat.getOccupantInfo()).isNotNull();
        assertThat(seat.getOccupantInfo().getName()).isEqualTo("Jane Doe");
    }

    @Test
    void getFloorPlan_teamOnlyMode_differentDepartment_hidesOccupantInfo() {
        when(locationConfigRepository.findByBuildingId(buildingId)).thenReturn(Optional.of(configWith(SeatVisibilityMode.TEAM_ONLY)));
        stubFloorAndBookings(fixedSeat(occupantId), List.of());

        User requester = User.builder().id(requestingUserId).ssoUserId("sso-req").firstName("Carol").lastName("White").email("carol@test.com").build();
        Employee requesterEmployee = Employee.builder()
                .id(UUID.randomUUID()).ssoUserId("sso-req").firstName("Carol").lastName("White")
                .department("Marketing").active(true).employmentStartDate(LocalDate.of(2021, 1, 1)).build();
        when(userRepository.findById(requestingUserId)).thenReturn(Optional.of(requester));
        when(employeeRepository.findBySsoUserId("sso-req")).thenReturn(Optional.of(requesterEmployee));
        when(employeeRepository.findAllById(any())).thenReturn(List.of(occupantEmployee));

        FloorPlanResponse result = service.getFloorPlan(buildingId, requestingUserId);

        FloorPlanSeatResponse seat = result.getFloors().getFirst().getSeats().getFirst();
        assertThat(seat.isOccupied()).isTrue();
        assertThat(seat.getOccupantInfo()).isNull();
    }

    @Test
    void getFloorPlan_teamOnlyMode_requesterWithoutEmployee_hidesOccupantInfo() {
        when(locationConfigRepository.findByBuildingId(buildingId)).thenReturn(Optional.of(configWith(SeatVisibilityMode.TEAM_ONLY)));
        stubFloorAndBookings(fixedSeat(occupantId), List.of());

        User requester = User.builder().id(requestingUserId).ssoUserId("sso-req").firstName("Liam").lastName("Gray").email("liam@test.com").build();
        when(userRepository.findById(requestingUserId)).thenReturn(Optional.of(requester));
        when(employeeRepository.findBySsoUserId("sso-req")).thenReturn(Optional.empty());
        when(employeeRepository.findAllById(any())).thenReturn(List.of(occupantEmployee));

        FloorPlanResponse result = service.getFloorPlan(buildingId, requestingUserId);

        FloorPlanSeatResponse seat = result.getFloors().getFirst().getSeats().getFirst();
        assertThat(seat.isOccupied()).isTrue();
        assertThat(seat.getOccupantInfo()).isNull();
    }

    @Test
    void getFloorPlan_availabilityOnlyMode_occupiedSeatHasNoOccupantInfo() {
        when(locationConfigRepository.findByBuildingId(buildingId)).thenReturn(Optional.of(configWith(SeatVisibilityMode.AVAILABILITY_ONLY)));
        stubFloorAndBookings(fixedSeat(occupantId), List.of());

        FloorPlanResponse result = service.getFloorPlan(buildingId, requestingUserId);

        FloorPlanSeatResponse seat = result.getFloors().getFirst().getSeats().getFirst();
        assertThat(seat.isOccupied()).isTrue();
        assertThat(seat.getOccupantInfo()).isNull();
    }

    @Test
    void getFloorPlan_unoccupiedSeat_isNotOccupied() {
        when(locationConfigRepository.findByBuildingId(buildingId)).thenReturn(Optional.of(configWith(SeatVisibilityMode.FULL)));
        stubFloorAndBookings(fixedSeat(null), List.of());

        FloorPlanResponse result = service.getFloorPlan(buildingId, requestingUserId);

        FloorPlanSeatResponse seat = result.getFloors().getFirst().getSeats().getFirst();
        assertThat(seat.isOccupied()).isFalse();
        assertThat(seat.getOccupantInfo()).isNull();
    }

    @Test
    void getFloorPlan_hotDeskWithActiveBooking_showsAsOccupied() {
        when(locationConfigRepository.findByBuildingId(buildingId)).thenReturn(Optional.of(configWith(SeatVisibilityMode.FULL)));
        stubFloorAndBookings(hotDeskSeat(), List.of(bookingFor(seatId, occupantId)));
        when(employeeRepository.findAllById(any())).thenReturn(List.of(occupantEmployee));

        FloorPlanResponse result = service.getFloorPlan(buildingId, requestingUserId);

        FloorPlanSeatResponse seat = result.getFloors().getFirst().getSeats().getFirst();
        assertThat(seat.isOccupied()).isTrue();
        assertThat(seat.getOccupantInfo()).isNotNull();
        assertThat(seat.getOccupantInfo().getName()).isEqualTo("Jane Doe");
    }

    @Test
    void getFloorPlan_hotDeskWithNoBooking_showsAsUnoccupied() {
        when(locationConfigRepository.findByBuildingId(buildingId)).thenReturn(Optional.of(configWith(SeatVisibilityMode.FULL)));
        stubFloorAndBookings(hotDeskSeat(), List.of());

        FloorPlanResponse result = service.getFloorPlan(buildingId, requestingUserId);

        FloorPlanSeatResponse seat = result.getFloors().getFirst().getSeats().getFirst();
        assertThat(seat.isOccupied()).isFalse();
        assertThat(seat.getOccupantInfo()).isNull();
    }

    @Test
    void getFloorPlan_configNotFound_throwsNotFoundException() {
        when(locationConfigRepository.findByBuildingId(buildingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getFloorPlan(buildingId, requestingUserId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getFloorPlan_multipleFloors_allIncluded() {
        UUID floorId2 = UUID.randomUUID();
        Floor floor2 = Floor.builder().id(floorId2).buildingId(buildingId).name("First Floor").floorNumber(1).active(true).build();
        Seat seat2 = Seat.builder().id(UUID.randomUUID()).floorId(floorId2).seatNumber("B1").seatType(SeatType.PERMANENT).status(SeatStatus.AVAILABLE).build();

        when(locationConfigRepository.findByBuildingId(buildingId)).thenReturn(Optional.of(configWith(SeatVisibilityMode.AVAILABILITY_ONLY)));
        when(floorRepository.findByBuildingIdAndActiveTrueAndDeletedAtIsNullOrderByFloorNumber(buildingId)).thenReturn(List.of(floor(), floor2));
        when(seatBookingRepository.findByBuildingIdAndBookingDateAndStatus(eq(buildingId), any(LocalDate.class), eq(SeatBookingStatus.CONFIRMED)))
                .thenReturn(List.of());
        when(seatRepository.findByFloorIdAndDeletedAtIsNull(floorId)).thenReturn(List.of(fixedSeat(null)));
        when(seatRepository.findByFloorIdAndDeletedAtIsNull(floorId2)).thenReturn(List.of(seat2));

        FloorPlanResponse result = service.getFloorPlan(buildingId, requestingUserId);

        assertThat(result.getFloors()).hasSize(2);
        assertThat(result.getFloors().get(0).getFloorName()).isEqualTo("Ground Floor");
        assertThat(result.getFloors().get(1).getFloorName()).isEqualTo("First Floor");
    }
}

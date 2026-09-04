package com.aoms.aomsbackend.attendance;

import com.aoms.aomsbackend.attendance.dto.CreateBlockReservationRequest;
import com.aoms.aomsbackend.attendance.dto.CreateSeatBookingRequest;
import com.aoms.aomsbackend.attendance.entity.*;
import com.aoms.aomsbackend.attendance.repository.BlockReservationRepository;
import com.aoms.aomsbackend.attendance.repository.RoomRepository;
import com.aoms.aomsbackend.audit.service.AuditLogService;
import com.aoms.aomsbackend.auth.constant.SessionAttribute;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.auth.service.UserRoleAccessService;
import com.aoms.aomsbackend.config.JwtTokenProvider;
import com.aoms.aomsbackend.seating.entity.Seat;
import com.aoms.aomsbackend.seating.entity.SeatBooking;
import com.aoms.aomsbackend.seating.entity.SeatBookingStatus;
import com.aoms.aomsbackend.seating.entity.SeatStatus;
import com.aoms.aomsbackend.seating.entity.SeatType;
import com.aoms.aomsbackend.seating.repository.SeatBookingRepository;
import com.aoms.aomsbackend.seating.repository.SeatRepository;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration test for the block reservation feature.
 *
 * <p>Verifies the full lifecycle:
 * <ol>
 *   <li>Manager creates a block for 3 seats → 3 CONFIRMED placeholder bookings.</li>
 *   <li>Team member claims one seat → placeholder updated in-place, blockReservationId preserved.</li>
 *   <li>Manager cancels the block → 2 placeholder bookings CANCELLED, 1 employee booking preserved.</li>
 * </ol>
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class BlockReservationIntegrationTest {

    @Autowired private WebApplicationContext context;

    @Autowired private BlockReservationRepository blockReservationRepository;
    @Autowired private SeatBookingRepository seatBookingRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private SeatRepository seatRepository;

    @MockitoBean private UserRoleAccessService userRoleAccessService;
    @MockitoBean private AuditLogService auditLogService;
    @MockitoBean private JwtDecoder jwtDecoder;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private MockMvc mockMvc;
    private JsonMapper jsonMapper;

    private final UUID managerId  = UUID.randomUUID();
    private final UUID employeeId = UUID.randomUUID();
    private final UUID buildingId = UUID.randomUUID();
    private final UUID floorId    = UUID.randomUUID();

    private UUID roomId;
    private final LocalDate bookingDate = LocalDate.now().plusDays(14);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        jsonMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

        // Seed a room (raw UUIDs, no FK enforcement in H2)
        Room room = Room.builder()
                .id(UUID.randomUUID())
                .buildingId(buildingId)
                .floorId(floorId)
                .roomName("Open Office A")
                .roomType("OPEN_OFFICE")
                .active(true)
                .build();
        room = roomRepository.save(room);
        roomId = room.getId();

        // Seed 3 seats in that room
        for (int i = 1; i <= 3; i++) {
            seatRepository.save(Seat.builder()
                    .roomId(roomId)
                    .floorId(floorId)
                    .buildingId(buildingId)
                    .seatNumber("S" + i)
                    .seatType(SeatType.HOT_DESK)
                    .status(SeatStatus.AVAILABLE)
                    .build());
        }

        // Manager has MANAGER role for the building; employee has EMPLOYEE role
        // Interceptor checks with null buildingId (no path variable on these endpoints);
        // service checks with the actual buildingId — any() covers both.
        when(userRoleAccessService.hasAccess(eq(managerId), any(), eq(UserRoleType.MANAGER)))
                .thenReturn(true);
        when(userRoleAccessService.hasAccess(any(), any(), eq(UserRoleType.EMPLOYEE)))
                .thenReturn(true);
    }

    @AfterEach
    void cleanUp() {
        seatBookingRepository.deleteAll();
        blockReservationRepository.deleteAll();
        seatRepository.deleteAll();
        roomRepository.deleteAll();
    }

    @Test
    void fullLifecycle_createBlock_claimSeat_cancelBlock_individualPreserved() throws Exception {
        // ── Step 1: Manager creates a block reservation for 3 seats ──────────

        CreateBlockReservationRequest blockRequest = new CreateBlockReservationRequest();
        blockRequest.setRoomId(roomId);
        blockRequest.setReservationDate(bookingDate);
        blockRequest.setSeatCount(3);
        blockRequest.setNotes("Team collab day");

        MvcResult createResult = mockMvc.perform(post("/api/v1/block-reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(blockRequest))
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), managerId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.seatCount").value(3))
                .andExpect(jsonPath("$.data.seatBookingIds.length()").value(3))
                .andReturn();

        // Extract the block reservation ID from the response
        String createBody = createResult.getResponse().getContentAsString();
        String blockIdStr = jsonMapper.readTree(createBody).path("data").path("id").asText();
        UUID blockId = UUID.fromString(blockIdStr);

        // Verify: 3 CONFIRMED placeholder bookings in DB
        List<SeatBooking> allBookings = seatBookingRepository.findByBlockReservationId(blockId);
        assertThat(allBookings).hasSize(3);
        assertThat(allBookings).allMatch(b -> b.getStatus() == SeatBookingStatus.CONFIRMED);
        assertThat(allBookings).allMatch(b -> b.getUserId().equals(managerId));
        assertThat(allBookings).allMatch(b -> b.getBlockReservationId().equals(blockId));

        // ── Step 2: Employee claims one seat from the block ─────────────────

        // Use the seat ID (not the booking ID) — the service looks up the placeholder by seatId + date
        UUID seatIdToClaim = allBookings.get(0).getSeatId();

        CreateSeatBookingRequest claimRequest = new CreateSeatBookingRequest();
        claimRequest.setSeatId(seatIdToClaim);
        claimRequest.setBuildingId(buildingId);
        claimRequest.setBookingDate(bookingDate);

        MvcResult claimResult = mockMvc.perform(post("/api/v1/seat-bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(claimRequest))
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), employeeId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.userId").value(employeeId.toString()))
                .andReturn();

        // Verify: the claimed seat now belongs to the employee
        String claimBody = claimResult.getResponse().getContentAsString();
        String claimedSeatBookingId = jsonMapper.readTree(claimBody).path("data").path("id").asText();
        SeatBooking claimed = seatBookingRepository.findById(UUID.fromString(claimedSeatBookingId)).orElseThrow();
        assertThat(claimed.getUserId()).isEqualTo(employeeId);
        assertThat(claimed.getStatus()).isEqualTo(SeatBookingStatus.CONFIRMED);
        assertThat(claimed.getBlockReservationId()).isEqualTo(blockId); // preserved

        // Verify: the other 2 remain as placeholders (manager-owned)
        List<SeatBooking> remaining = seatBookingRepository.findByBlockReservationId(blockId);
        assertThat(remaining).hasSize(3); // still 3 total
        long placeholderCount = remaining.stream()
                .filter(b -> b.getUserId().equals(managerId) && b.getStatus() == SeatBookingStatus.CONFIRMED)
                .count();
        assertThat(placeholderCount).isEqualTo(2);

        // ── Step 3: Manager cancels the block ───────────────────────────────

        mockMvc.perform(delete("/api/v1/block-reservations/{id}", blockId)
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), managerId.toString()))
                .andExpect(status().isOk());

        // Verify: block reservation is soft-cancelled (not deleted)
        BlockReservation cancelledBlock = blockReservationRepository.findById(blockId).orElseThrow();
        assertThat(cancelledBlock.getStatus()).isEqualTo(BlockReservationStatus.CANCELLED);

        // Verify: all 3 bookings still exist — 2 placeholders CANCELLED, employee's preserved
        List<SeatBooking> afterCancel = seatBookingRepository.findAll();
        assertThat(afterCancel).hasSize(3);

        long cancelledCount = afterCancel.stream()
                .filter(b -> b.getUserId().equals(managerId) && b.getStatus() == SeatBookingStatus.CANCELLED)
                .count();
        assertThat(cancelledCount).isEqualTo(2);

        SeatBooking employeeSeat = afterCancel.stream()
                .filter(b -> b.getUserId().equals(employeeId))
                .findFirst()
                .orElseThrow();
        assertThat(employeeSeat.getStatus()).isEqualTo(SeatBookingStatus.CONFIRMED);
        assertThat(employeeSeat.getBlockReservationId()).isEqualTo(blockId); // reference preserved
    }
}

package com.aoms.aomsbackend.seating;

import com.aoms.aomsbackend.seating.entity.SeatBookingStatus;
import com.aoms.aomsbackend.seating.entity.SeatBooking;
import com.aoms.aomsbackend.seating.repository.SeatBookingRepository;
import com.aoms.aomsbackend.auth.constant.SessionAttribute;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.auth.service.UserRoleAccessService;
import com.aoms.aomsbackend.config.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for seat booking cancellation (AOMS-116).
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class SeatBookingIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SeatBookingRepository bookingRepository;

    @MockitoBean
    private UserRoleAccessService userRoleAccessService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private MockMvc mockMvc;

    private final UUID employeeId = UUID.randomUUID();
    private final UUID buildingId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void cleanUp() {
        bookingRepository.deleteAll();
    }

    @Test
    void cancelBooking_employeeCancelsOwnFutureBooking_seatBecomesAvailable() throws Exception {
        when(userRoleAccessService.hasAccess(employeeId, buildingId, UserRoleType.EMPLOYEE))
                .thenReturn(true);

        UUID seatId = UUID.randomUUID();
        LocalDate futureDate = LocalDate.now().plusDays(10);

        SeatBooking booking = SeatBooking.builder()
                .seatId(seatId)
                .userId(employeeId)
                .buildingId(buildingId)
                .bookingDate(futureDate)
                .status(SeatBookingStatus.CONFIRMED)
                .build();
        SeatBooking saved = bookingRepository.save(booking);

        assertThat(bookingRepository.existsBySeatIdAndBookingDateAndStatus(
                seatId, futureDate, SeatBookingStatus.CONFIRMED)).isTrue();

        mockMvc.perform(delete("/api/v1/locations/{buildingId}/bookings/{bookingId}",
                                buildingId, saved.getId())
                        .sessionAttr(SessionAttribute.USER_ID.getKey(), employeeId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancelledAt").isNotEmpty());

        SeatBooking updated = bookingRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(SeatBookingStatus.CANCELLED);
        assertThat(updated.getCancelledAt()).isNotNull();

        assertThat(bookingRepository.existsBySeatIdAndBookingDateAndStatus(
                seatId, futureDate, SeatBookingStatus.CONFIRMED)).isFalse();
    }
}

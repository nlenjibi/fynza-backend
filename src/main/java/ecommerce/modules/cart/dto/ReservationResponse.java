package ecommerce.modules.cart.dto;

import ecommerce.modules.cart.entity.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {

    private UUID reservationId;
    private ReservationStatus status;
    private Long estimatedConfirmationMs;
    private String message;
    private int retryCount;

    public static ReservationResponse pending(UUID reservationId, long estimatedMs) {
        return ReservationResponse.builder()
                .reservationId(reservationId)
                .status(ReservationStatus.PENDING)
                .estimatedConfirmationMs(estimatedMs)
                .message("Stock reservation is being processed")
                .retryCount(0)
                .build();
    }

    public static ReservationResponse confirmed(UUID reservationId) {
        return ReservationResponse.builder()
                .reservationId(reservationId)
                .status(ReservationStatus.CONFIRMED)
                .message("Stock reserved successfully")
                .build();
    }

    public static ReservationResponse failed(UUID reservationId, String message, int retryCount) {
        return ReservationResponse.builder()
                .reservationId(reservationId)
                .status(ReservationStatus.FAILED)
                .message(message)
                .retryCount(retryCount)
                .build();
    }
}

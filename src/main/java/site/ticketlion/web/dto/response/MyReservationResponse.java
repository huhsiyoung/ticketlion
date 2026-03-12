package site.ticketlion.web.dto.response;

import java.time.Instant;
import java.time.LocalDateTime;
import org.springframework.cglib.core.Local;
import site.ticketlion.domain.Reservation;
import site.ticketlion.domain.ReservationStatus;

public record MyReservationResponse(
    Long id,
    String eventTitle,
    String eventVenue,
    LocalDateTime eventDate,
    String seatNo,
    Instant reservedAt,
    ReservationStatus status
) {
    public static MyReservationResponse from(Reservation r) {
        return new MyReservationResponse(
            r.getId(),
            r.getSeat().getEvent().getTitle(),
            r.getSeat().getEvent().getVenue(),
            r.getSeat().getEvent().getStartAt(),
            r.getSeat().getSeatNo(),
            r.getReservedAt(),
            r.getStatus()
        );
    }
}

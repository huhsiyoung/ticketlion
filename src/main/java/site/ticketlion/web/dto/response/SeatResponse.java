package site.ticketlion.web.dto.response;

import site.ticketlion.domain.Seat;
import site.ticketlion.domain.SeatStatus;

public record SeatResponse(String seatNo, SeatStatus status) {
    public static SeatResponse from(Seat s) {
        return new SeatResponse(s.getSeatNo(), s.getStatus());
    }
}
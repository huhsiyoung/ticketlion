package site.ticketlion.web.dto.response;

import site.ticketlion.domain.SeatStatus;

public record SeatDto(
    Long seatId,
    String seatNo,
    SeatStatus status
) {}

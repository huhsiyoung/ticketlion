package site.ticketlion.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReserveResponse {
    private Long reservationId;
    private Long seatId;
    private Long userId;
}

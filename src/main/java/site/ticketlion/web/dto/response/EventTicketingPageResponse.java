package site.ticketlion.web.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import site.ticketlion.domain.SeatStatus;

public record EventTicketingPageResponse(
    Long eventId,
    String title,
    LocalDateTime startAt,
    String venue,
    int price,
    List<SeatDto> seats
) { }
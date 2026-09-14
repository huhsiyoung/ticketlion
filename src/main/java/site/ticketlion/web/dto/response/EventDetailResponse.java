package site.ticketlion.web.dto.response;

import site.ticketlion.domain.Event;

public record EventDetailResponse(
    Event event,
    int totalSeats,
    int availableSeats
) {}

package site.ticketlion.web.dto.response;

import java.time.LocalDateTime;
import site.ticketlion.domain.Event;
import site.ticketlion.domain.EventStatus;

public record EventResponse(
    Long id,
    String title,
    LocalDateTime startAt,
    String category,
    String venue,
    Integer price,
    EventStatus status,
    String themeColor,
    String thumbnailEmoji
) {
    public static EventResponse from(Event e) {
        return new EventResponse(
            e.getId(),
            e.getTitle(),
            e.getStartAt(),
            e.getCategory(),
            e.getVenue(),
            e.getPrice(),
            e.getStatus(),
            e.getThemeColor(),
            e.getThumbnailEmoji()
        );
    }
}
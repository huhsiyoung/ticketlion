package site.ticketlion.web.dto.request;

import java.time.LocalDateTime;

public record EventUpdateRequest(
    String title,
    LocalDateTime startAt,
    String category,
    String venue,
    Integer price,
    String themeColor,
    String thumbnailEmoji
) {}

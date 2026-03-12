package site.ticketlion.web.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;
import site.ticketlion.domain.Member;
import site.ticketlion.domain.MemberRole;

public record MemberResponse(
    UUID id,
    String name,
    String email,
    String phone,
    MemberRole role,
    LocalDateTime createdAt
) {
    public static MemberResponse from(Member m) {
        return new MemberResponse(
            m.getId(),
            m.getName(),
            m.getEmail(),
            m.getPhone(),
            m.getRole(),
            m.getCreatedAt()
        );
    }
}

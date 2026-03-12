package site.ticketlion.service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import site.ticketlion.web.dto.response.SeatDto;

@Service
@RequiredArgsConstructor
public class SeatHoldService {

    private final Duration HOLD_TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, Object> redisTemplate;

    private final StringRedisTemplate stringRedisTemplate;

    private final DefaultRedisScript<Long> holdSeatsScript;

    public boolean holdOne(Long eventId, String seatId, UUID userId) {
        String key = seatHoldKey(eventId, seatId);

        Boolean result = stringRedisTemplate.opsForValue()
            .setIfAbsent(key, String.valueOf(userId), HOLD_TTL);

        return Boolean.TRUE.equals(result);
    }

    private String seatHoldKey(long eventId, String seatId) {
        return "seat:hold:" + eventId + ":" + seatId;
    }

    public boolean holdMany(Long eventId, List<SeatDto> seatIds, UUID userId) { // seatId? seatNo?
        List<String> keys = seatIds.stream()
            .map(seatDto -> seatHoldKey(eventId, seatDto.seatNo()))
            .collect(Collectors.toList());

        Long result = redisTemplate.execute(
            holdSeatsScript,
            keys,
            String.valueOf(userId),
            String.valueOf(HOLD_TTL.toSeconds())
        );

        return result != null && result == 1L;
    }

    public boolean isOwner(Long eventId, String seatId, UUID userId) {
        String key = seatHoldKey(eventId, seatId);

        String value = stringRedisTemplate.opsForValue().get(key);

        if (value == null) {
            return false;
        }

        return value.equals(String.valueOf(userId));
    }
}

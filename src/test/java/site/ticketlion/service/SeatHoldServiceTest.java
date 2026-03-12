package site.ticketlion.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import site.ticketlion.repository.SeatRepository;
import site.ticketlion.web.dto.response.SeatDto;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Profile("test")
class SeatHoldServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private DefaultRedisScript<Long> holdSeatsScript;

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private SeatHoldService seatHoldService;

    @Test
    @DisplayName("좌석 점유 성공 - 단일 좌석")
    void holdOne_success() {
        // given
        Long eventId = 1L;
        String seatId = "A1";
        UUID userId = UUID.randomUUID();
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        // when
        boolean result = seatHoldService.holdOne(eventId, seatId, userId);

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("좌석 점유 실패 - 단일 좌석 (이미 점유됨)")
    void holdOne_fail_already_held() {
        // given
        Long eventId = 1L;
        String seatId = "A1";
        UUID userId = UUID.randomUUID();
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        // when
        boolean result = seatHoldService.holdOne(eventId, seatId, userId);

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("좌석 점유 성공 - 다중 좌석")
    void holdMany_success() {
        // given
        Long eventId = 1L;
        List<SeatDto> seats = List.of(new SeatDto(1L, "A1", null), new SeatDto(2L, "A2", null));
        UUID userId = UUID.randomUUID();

        when(redisTemplate.execute(
            any(DefaultRedisScript.class),
            anyList(),
            any(),
            any()
        )).thenReturn(1L);

        // when
        boolean result = seatHoldService.holdMany(eventId, seats, userId);

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("좌석 점유 실패 - 다중 좌석")
    void holdMany_fail() {
        // given
        Long eventId = 1L;
        List<SeatDto> seats = List.of(new SeatDto(1L, "A1", null), new SeatDto(2L, "A2", null));
        UUID userId = UUID.randomUUID();

        // no stub needed: default mock returns null, holdMany returns false for any non-1L result

        // when
        boolean result = seatHoldService.holdMany(eventId, seats, userId);

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("좌석 소유 확인 성공")
    void isOwner_success() {
        // given
        Long eventId = 1L;
        String seatId = "A1";
        UUID userId = UUID.randomUUID();
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(userId.toString());

        // when
        boolean result = seatHoldService.isOwner(eventId, seatId, userId);

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("좌석 소유 확인 실패 - 소유자 불일치")
    void isOwner_fail_mismatch() {
        // given
        Long eventId = 1L;
        String seatId = "A1";
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(otherUserId.toString());

        // when
        boolean result = seatHoldService.isOwner(eventId, seatId, userId);

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("좌석 소유 확인 실패 - 점유 정보 없음")
    void isOwner_fail_not_found() {
        // given
        Long eventId = 1L;
        String seatId = "A1";
        UUID userId = UUID.randomUUID();
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        // when
        boolean result = seatHoldService.isOwner(eventId, seatId, userId);

        // then
        assertFalse(result);
    }
}

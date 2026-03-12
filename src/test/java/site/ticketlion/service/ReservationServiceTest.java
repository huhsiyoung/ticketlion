package site.ticketlion.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.test.context.ActiveProfiles;
import site.ticketlion.domain.Event;
import site.ticketlion.domain.Reservation;
import site.ticketlion.domain.ReservationStatus;
import site.ticketlion.domain.Seat;
import site.ticketlion.domain.SeatStatus;
import site.ticketlion.repository.ReservationRepository;
import site.ticketlion.repository.SeatRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private DefaultRedisScript<Long> holdSeatsScript;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    @DisplayName("좌석 예매 성공")
    void reserveSeats_success() {
        // given
        Long eventId = 1L;
        List<String> seatNumbers = List.of("A1");
        UUID memberId = UUID.randomUUID();

        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

        Seat seat = new Seat(new Event(), "A1", SeatStatus.AVAILABLE);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(memberId.toString());

        when(seatRepository.findByEventIdAndSeatNoIn(eventId, seatNumbers))
            .thenReturn(List.of(seat));

        // saveAll 호출 시 id가 있는 Reservation 반환하도록 설정
        when(reservationRepository.saveAll(anyList()))
            .thenAnswer(invocation -> {
                List<Reservation> reservations = invocation.getArgument(0);
                Reservation r = reservations.get(0);

                return List.of(new Reservation(
                    1L,
                    r.getSeat(),
                    r.getUserId(),
                    r.getReservedAt(),
                    r.getStatus()
                ));
            });

        // when
        Long reservationId = reservationService.reserveSeats(eventId, seatNumbers, memberId);

        // then
        assertNotNull(reservationId);
        assertEquals(1L, reservationId);
        assertEquals(SeatStatus.RESERVED, seat.getStatus());
        verify(reservationRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("좌석 예매 실패 - 선점 시간 만료")
    void reserveSeats_fail_hold_expired() {
        // given
        Long eventId = 1L;
        List<String> seatNumbers = List.of("A1");
        UUID memberId = UUID.randomUUID();
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        // when & then
        assertThrows(IllegalStateException.class, () -> reservationService.reserveSeats(eventId, seatNumbers, memberId));
    }

    @Test
    @DisplayName("예매 취소 성공")
    void cancelReservation_success() {
        // given
        Long reservationId = 1L;
        UUID memberId = UUID.randomUUID();
        Event event = new Event();
        Seat seat = new Seat(event, "A1", SeatStatus.RESERVED);
        Reservation reservation = new Reservation(memberId, Instant.now(), seat);
        reservation.confirm();

        when(reservationRepository.findByIdWithSeatAndEvent(reservationId)).thenReturn(Optional.of(reservation));

        // when
        reservationService.cancelReservation(reservationId, memberId);

        // then
        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(SeatStatus.AVAILABLE, seat.getStatus());
        verify(stringRedisTemplate).delete(anyString());
    }

    @Test
    @DisplayName("예매 취소 실패 - 예매자 정보 불일치")
    void cancelReservation_fail_user_mismatch() {
        // given
        Long reservationId = 1L;
        UUID memberId = UUID.randomUUID();
        UUID otherMemberId = UUID.randomUUID();
        Event event = new Event();
        Seat seat = new Seat(event, "A1", SeatStatus.RESERVED);
        Reservation reservation = new Reservation(otherMemberId, Instant.now(), seat);

        when(reservationRepository.findByIdWithSeatAndEvent(reservationId)).thenReturn(Optional.of(reservation));

        // when & then
        assertThrows(IllegalStateException.class, () -> reservationService.cancelReservation(reservationId, memberId));
    }
}
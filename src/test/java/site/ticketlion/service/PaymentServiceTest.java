package site.ticketlion.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.ticketlion.domain.Event;
import site.ticketlion.domain.Payment;
import site.ticketlion.domain.PaymentStatus;
import site.ticketlion.domain.Reservation;
import site.ticketlion.domain.ReservationStatus;
import site.ticketlion.domain.Seat;
import site.ticketlion.domain.SeatStatus;
import site.ticketlion.repository.PaymentRepository;
import site.ticketlion.repository.ReservationRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("멱등키 중복 요청 - 기존 Payment 반환")
    void pay_idempotent_returns_existing_payment() {
        // given
        UUID userId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        Payment existingPayment = Payment.builder()
            .id(1L)
            .reservationId(10L)
            .userId(userId)
            .amount(0L)
            .status(PaymentStatus.SUCCESS)
            .idempotencyKey(idempotencyKey.toString())
            .provider("FREE")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(paymentRepository.findByIdempotencyKey(idempotencyKey.toString()))
            .thenReturn(Optional.of(existingPayment));

        // when
        Payment result = paymentService.pay(10L, userId, idempotencyKey);

        // then
        assertSame(existingPayment, result);
        verify(reservationRepository, never()).findById(any());
    }

    @Test
    @DisplayName("결제 성공 - Payment 저장 및 Reservation CONFIRMED 변경")
    void pay_success() {
        // given
        UUID userId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        Long reservationId = 1L;

        Seat seat = new Seat(new Event(), "A1", SeatStatus.RESERVED);
        Reservation reservation = new Reservation(userId, Instant.now(), seat);

        when(paymentRepository.findByIdempotencyKey(idempotencyKey.toString()))
            .thenReturn(Optional.empty());
        when(reservationRepository.findById(reservationId))
            .thenReturn(Optional.of(reservation));

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);

        // when
        Payment result = paymentService.pay(reservationId, userId, idempotencyKey);

        // then
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment saved = paymentCaptor.getValue();
        assertEquals(0L, saved.getAmount());
        assertEquals("FREE", saved.getProvider());
        assertEquals(idempotencyKey.toString(), saved.getIdempotencyKey());

        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
    }

    @Test
    @DisplayName("결제 실패 - 예매 정보 없음")
    void pay_fail_reservation_not_found() {
        // given
        UUID userId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        Long reservationId = 99L;

        when(paymentRepository.findByIdempotencyKey(idempotencyKey.toString()))
            .thenReturn(Optional.empty());
        when(reservationRepository.findById(reservationId))
            .thenReturn(Optional.empty());

        // when & then
        assertThrows(IllegalStateException.class,
            () -> paymentService.pay(reservationId, userId, idempotencyKey));
    }

    @Test
    @DisplayName("결제 실패 - 예매자 정보 불일치")
    void pay_fail_user_mismatch() {
        // given
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        Long reservationId = 1L;

        Seat seat = new Seat(new Event(), "A1", SeatStatus.RESERVED);
        Reservation reservation = new Reservation(otherUserId, Instant.now(), seat);

        when(paymentRepository.findByIdempotencyKey(idempotencyKey.toString()))
            .thenReturn(Optional.empty());
        when(reservationRepository.findById(reservationId))
            .thenReturn(Optional.of(reservation));

        // when & then
        assertThrows(IllegalStateException.class,
            () -> paymentService.pay(reservationId, userId, idempotencyKey));
    }

    @Test
    @DisplayName("결제 실패 - 이미 CONFIRMED 상태")
    void pay_fail_already_confirmed() {
        // given
        UUID userId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        Long reservationId = 1L;

        Seat seat = new Seat(new Event(), "A1", SeatStatus.RESERVED);
        Reservation reservation = new Reservation(userId, Instant.now(), seat);
        reservation.confirm();

        when(paymentRepository.findByIdempotencyKey(idempotencyKey.toString()))
            .thenReturn(Optional.empty());
        when(reservationRepository.findById(reservationId))
            .thenReturn(Optional.of(reservation));

        // when & then
        assertThrows(IllegalStateException.class,
            () -> paymentService.pay(reservationId, userId, idempotencyKey));
    }

    @Test
    @DisplayName("결제 실패 - PENDING 외 상태 (CANCELLED)")
    void pay_fail_not_pending_status() {
        // given
        UUID userId = UUID.randomUUID();
        UUID idempotencyKey = UUID.randomUUID();
        Long reservationId = 1L;

        Seat seat = new Seat(new Event(), "A1", SeatStatus.AVAILABLE);
        Reservation reservation = new Reservation(userId, Instant.now(), seat);
        reservation.cancel();

        when(paymentRepository.findByIdempotencyKey(idempotencyKey.toString()))
            .thenReturn(Optional.empty());
        when(reservationRepository.findById(reservationId))
            .thenReturn(Optional.of(reservation));

        // when & then
        assertThrows(IllegalStateException.class,
            () -> paymentService.pay(reservationId, userId, idempotencyKey));
    }
}
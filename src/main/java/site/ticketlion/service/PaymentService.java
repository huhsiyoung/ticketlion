package site.ticketlion.service;

import java.time.Instant;
import java.util.UUID;
import site.ticketlion.domain.Payment;
import site.ticketlion.domain.PaymentStatus;
import site.ticketlion.domain.Reservation;
import site.ticketlion.domain.ReservationStatus;
import site.ticketlion.repository.PaymentRepository;
import site.ticketlion.repository.ReservationRepository;

public class PaymentService {

    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;

    public PaymentService(
        ReservationRepository reservationRepository,
        PaymentRepository paymentRepository
    ) {
        this.reservationRepository = reservationRepository;
        this.paymentRepository = paymentRepository;
    }

    public Payment pay(Long reservationId, UUID userId, UUID idempotencyKey) {
        return paymentRepository
            .findByIdempotencyKey(idempotencyKey.toString())
            .orElseGet(() -> createPayment(reservationId, userId, idempotencyKey));
    }

    private Payment createPayment(Long reservationId, UUID userId, UUID idempotencyKey) {

        Reservation reservation = reservationRepository
            .findById(reservationId)
            .orElseThrow();

        if (!reservation.getUserId().equals(userId)) {
            throw new IllegalStateException("예매자 정보가 일치하지 않습니다.");
        }

        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            throw new IllegalStateException("이미 결제 완료된 예매입니다.");
        }

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException(
                "잘못된 예약 상태입니다: " + reservation.getStatus()
            );
        }

        Instant now = Instant.now();

        Payment payment = Payment.builder()
            .reservationId(reservationId)
            .userId(userId)
            .amount(0L)
            .status(PaymentStatus.READY)
            .idempotencyKey(idempotencyKey.toString())
            .provider("FREE")
            .createdAt(now)
            .updatedAt(now)
            .build();

        paymentRepository.save(payment);

        payment.success();
        payment.update(Instant.now());

        reservation.confirm();

        return payment;
    }
}
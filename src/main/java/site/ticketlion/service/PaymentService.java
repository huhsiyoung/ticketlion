package site.ticketlion.service;

import java.time.Instant;
import java.util.UUID;

import lombok.Generated;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import site.ticketlion.domain.Payment;
import site.ticketlion.domain.PaymentStatus;
import site.ticketlion.domain.Reservation;
import site.ticketlion.domain.ReservationStatus;
import site.ticketlion.repository.PaymentRepository;
import site.ticketlion.repository.ReservationRepository;

@Service
public class PaymentService {

    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public Payment pay(Long reservationId, UUID userId, UUID idempotencyKey) {

        return (Payment) this.paymentRepository
            .findByIdempotencyKey(idempotencyKey.toString())
            .orElseGet(() -> this.createPayment(reservationId, userId, idempotencyKey));
    }

    private Payment createPayment(Long reservationId, UUID userId, UUID idempotencyKey) {

        Reservation reservation =
            (Reservation) this.reservationRepository
                .findById(reservationId)
                .orElseThrow(() ->
                    new IllegalStateException("예매 정보를 찾을 수 없습니다.")
                );

        if (!reservation.getUserId().equals(userId)) {

            throw new IllegalStateException("예매자 정보가 일치하지 않습니다.");

        } else if (reservation.getStatus() == ReservationStatus.CONFIRMED) {

            throw new IllegalStateException("이미 결제 완료된 예매입니다.");

        } else if (reservation.getStatus() != ReservationStatus.PENDING) {

            throw new IllegalStateException(
                "결제 가능한 상태가 아닙니다. 상태: "
                    + String.valueOf(reservation.getStatus())
            );

        } else {

            Instant now = Instant.now();

            Payment payment =
                Payment.builder()
                    .reservationId(reservationId)
                    .userId(userId)
                    .amount(0L)
                    .status(PaymentStatus.READY)
                    .idempotencyKey(idempotencyKey.toString())
                    .provider("FREE")
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            this.paymentRepository.save(payment);

            payment.success();
            payment.update(Instant.now());

            reservation.confirm();

            return payment;
        }
    }

    @Generated
    public PaymentService(
        final ReservationRepository reservationRepository,
        final PaymentRepository paymentRepository
    ) {
        this.reservationRepository = reservationRepository;
        this.paymentRepository = paymentRepository;
    }
}
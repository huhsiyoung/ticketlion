package site.ticketlion.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import site.ticketlion.domain.Payment;
import site.ticketlion.domain.PaymentStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("멱등키로 결제 정보 조회")
    void findByIdempotencyKey() {
        // given
        String idempotencyKey = UUID.randomUUID().toString();
        Payment payment = Payment.builder()
                .reservationId(1L)
                .userId(UUID.randomUUID())
                .amount(10000L)
                .status(PaymentStatus.SUCCESS)
                .idempotencyKey(idempotencyKey)
                .provider("MOCK")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        paymentRepository.save(payment);

        // when
        Optional<Payment> foundPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);

        // then
        assertTrue(foundPayment.isPresent());
        assertEquals(idempotencyKey, foundPayment.get().getIdempotencyKey());
    }

    @Test
    @DisplayName("예약 ID와 상태로 결제 존재 여부 확인")
    void existsByReservationIdAndStatus() {
        // given
        Long reservationId = 1L;
        Payment payment = Payment.builder()
                .reservationId(reservationId)
                .userId(UUID.randomUUID())
                .amount(10000L)
                .status(PaymentStatus.SUCCESS)
                .idempotencyKey(UUID.randomUUID().toString())
                .provider("MOCK")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        paymentRepository.save(payment);

        // when
        boolean exists = paymentRepository.existsByReservationIdAndStatus(reservationId, PaymentStatus.SUCCESS);

        // then
        assertTrue(exists);
    }
}

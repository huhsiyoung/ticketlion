package site.ticketlion.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import site.ticketlion.domain.Payment;
import site.ticketlion.domain.PaymentStatus;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    boolean existsByReservationIdAndStatus(Long reservationId, PaymentStatus status);
}

package site.ticketlion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "payments",
    uniqueConstraints = {
        @UniqueConstraint(name="uk_payment_idempotency", columnNames = {"idempotency_key"})
    },
    indexes = {
        @Index(name="idx_payment_reservation", columnList = "reservation_id")
    }
)
public class Payment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="reservation_id", nullable=false)
    private Long reservationId;

    @Column(name="user_id", nullable=false)
    private UUID userId;

    @Column(name="amount", nullable=false)
    private Long amount; // 연습용: 임의 금액

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private PaymentStatus status;

    @Column(name="idempotency_key", nullable=false, length=64)
    private String idempotencyKey;

    @Column(name="provider", nullable=false)
    private String provider; // "MOCK"

    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt;

    @Column(name="updated_at", nullable=false)
    private Instant updatedAt;

    public void success() {
        this.status = PaymentStatus.SUCCESS;
    }

    public void fail() {
        this.status = PaymentStatus.FAILED;
    }

    public void update(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

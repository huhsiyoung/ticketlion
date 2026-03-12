package site.ticketlion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.ticketlion.web.exception.SeatAlreadyReservedException;
import site.ticketlion.web.exception.SeatNotReservedException;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "seats",
    indexes = {
        @Index(name = "idx_seat_event", columnList = "event_id"),
        @Index(name = "idx_seat_status", columnList = "status")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_event_seatno", columnNames = {"event_id", "seat_no"})
    }
)
public class Seat {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "seat_no", nullable = false)
    private String seatNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status;

    public Seat(Event event, String seatNo, SeatStatus status) {
        this.event = event;
        this.seatNo = seatNo;
        this.status = status;
    }

    public void reserve() {
        if (this.status != SeatStatus.AVAILABLE) {
            throw new SeatAlreadyReservedException(this.seatNo);
        }

        this.status = SeatStatus.RESERVED;
    }

    public void release() {
        if (this.status != SeatStatus.RESERVED) {
            throw new SeatNotReservedException(this.seatNo);
        }

        this.status = SeatStatus.AVAILABLE;
    }
}

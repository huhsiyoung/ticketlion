package site.ticketlion.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import site.ticketlion.domain.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ReservationRepositoryTest {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Test
    @DisplayName("ID로 예매 정보와 좌석, 이벤트 정보 함께 조회")
    void findByIdWithSeatAndEvent() {
        // given
        Event event = new Event(null, "Test Event", LocalDateTime.now(), "CONCERT", "Venue", 10000, null, null, EventStatus.ACTIVE, "#FFFFFF", "😀");
        eventRepository.save(event);
        Seat seat = new Seat(event, "A1", SeatStatus.RESERVED);
        seatRepository.save(seat);
        Reservation reservation = new Reservation(UUID.randomUUID(), Instant.now(), seat);
        reservationRepository.save(reservation);

        // when
        Optional<Reservation> foundReservation = reservationRepository.findByIdWithSeatAndEvent(reservation.getId());

        // then
        assertTrue(foundReservation.isPresent());
        assertNotNull(foundReservation.get().getSeat());
        assertNotNull(foundReservation.get().getSeat().getEvent());
    }

    @Test
    @DisplayName("특정 시간 이전의 PENDING 상태 예매 조회")
    void findByStatusAndReservedAtBefore() {
        // given
        Event event = new Event(null, "Test Event", LocalDateTime.now(), "CONCERT", "Venue", 10000, null, null, EventStatus.ACTIVE, "#FFFFFF", "😀");
        eventRepository.save(event);
        Seat seat1 = new Seat(event, "A1", SeatStatus.RESERVED);
        seatRepository.save(seat1);
        Reservation oldPending = new Reservation(UUID.randomUUID(), Instant.now().minusSeconds(3600), seat1);
        reservationRepository.save(oldPending);

        Seat seat2 = new Seat(event, "A2", SeatStatus.RESERVED);
        seatRepository.save(seat2);
        Reservation newPending = new Reservation(UUID.randomUUID(), Instant.now(), seat2);
        reservationRepository.save(newPending);

        // when
        List<Reservation> oldPendings = reservationRepository.findByStatusAndReservedAtBefore(ReservationStatus.PENDING, Instant.now().minusSeconds(1800));

        // then
        assertEquals(1, oldPendings.size());
        assertEquals(oldPending.getId(), oldPendings.get(0).getId());
    }

    @Test
    @DisplayName("동일 배치 예매 조회")
    void findBatchReservations() {
        // given
        UUID userId = UUID.randomUUID();
        Instant reservedAt = Instant.now();
        Event event = new Event(null, "Test Event", LocalDateTime.now(), "CONCERT", "Venue", 10000, null, null, EventStatus.ACTIVE, "#FFFFFF", "😀");
        eventRepository.save(event);
        Seat seat1 = new Seat(event, "A1", SeatStatus.RESERVED);
        seatRepository.save(seat1);
        Reservation reservation1 = new Reservation(userId, reservedAt, seat1);
        reservationRepository.save(reservation1);

        Seat seat2 = new Seat(event, "A2", SeatStatus.RESERVED);
        seatRepository.save(seat2);
        Reservation reservation2 = new Reservation(userId, reservedAt, seat2);
        reservationRepository.save(reservation2);

        // when
        List<Reservation> batchReservations = reservationRepository.findBatchReservations(userId, event.getId(), reservedAt);

        // then
        assertEquals(2, batchReservations.size());
    }

    @Test
    @DisplayName("특정 회원의 모든 예매 내역 조회")
    void findAllByUserIdWithSeatAndEvent() {
        // given
        UUID userId = UUID.randomUUID();
        Event event = new Event(null, "Test Event", LocalDateTime.now(), "CONCERT", "Venue", 10000, null, null, EventStatus.ACTIVE, "#FFFFFF", "😀");
        eventRepository.save(event);
        Seat seat1 = new Seat(event, "A1", SeatStatus.RESERVED);
        seatRepository.save(seat1);
        Reservation reservation1 = new Reservation(userId, Instant.now(), seat1);
        reservationRepository.save(reservation1);

        Seat seat2 = new Seat(event, "A2", SeatStatus.RESERVED);
        seatRepository.save(seat2);
        Reservation reservation2 = new Reservation(userId, Instant.now(), seat2);
        reservationRepository.save(reservation2);

        // when
        List<Reservation> myReservations = reservationRepository.findAllByUserIdWithSeatAndEvent(userId);

        // then
        assertEquals(2, myReservations.size());
    }
}

package site.ticketlion.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import site.ticketlion.domain.Event;
import site.ticketlion.domain.EventStatus;
import site.ticketlion.domain.Seat;
import site.ticketlion.domain.SeatStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class SeatRepositoryTest {

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private EventRepository eventRepository;

    @Test
    @DisplayName("이벤트 ID로 모든 좌석 삭제")
    void deleteByEventId() {
        // given
        Event event = new Event(null, "Test Event", LocalDateTime.now(), "CONCERT", "Venue", 10000, null, null, EventStatus.ACTIVE, "#FFFFFF", "😀");
        eventRepository.save(event);
        Seat seat1 = new Seat(event, "A1", SeatStatus.AVAILABLE);
        Seat seat2 = new Seat(event, "A2", SeatStatus.AVAILABLE);
        seatRepository.saveAll(List.of(seat1, seat2));

        // when
        seatRepository.deleteByEventId(event.getId());

        // then
        List<Seat> seats = seatRepository.findAllByEventId(event.getId());
        assertTrue(seats.isEmpty());
    }

    @Test
    @DisplayName("이벤트 ID로 모든 좌석 조회")
    void findAllByEventId() {
        // given
        Event event = new Event(null, "Test Event", LocalDateTime.now(), "CONCERT", "Venue", 10000, null, null, EventStatus.ACTIVE, "#FFFFFF", "😀");
        eventRepository.save(event);
        Seat seat1 = new Seat(event, "A1", SeatStatus.AVAILABLE);
        Seat seat2 = new Seat(event, "A2", SeatStatus.AVAILABLE);
        seatRepository.saveAll(List.of(seat1, seat2));

        // when
        List<Seat> seats = seatRepository.findAllByEventId(event.getId());

        // then
        assertEquals(2, seats.size());
    }

    @Test
    @DisplayName("이벤트 ID와 좌석 번호로 좌석 조회")
    void findByEventIdAndSeatNo() {
        // given
        Event event = new Event(null, "Test Event", LocalDateTime.now(), "CONCERT", "Venue", 10000, null, null, EventStatus.ACTIVE, "#FFFFFF", "😀");
        eventRepository.save(event);
        Seat seat = new Seat(event, "A1", SeatStatus.AVAILABLE);
        seatRepository.save(seat);

        // when
        Optional<Seat> foundSeat = seatRepository.findByEventIdAndSeatNo(event.getId(), "A1");

        // then
        assertTrue(foundSeat.isPresent());
        assertEquals("A1", foundSeat.get().getSeatNo());
    }

    @Test
    @DisplayName("이벤트 ID와 좌석 번호 리스트로 좌석 조회")
    void findByEventIdAndSeatNoIn() {
        // given
        Event event = new Event(null, "Test Event", LocalDateTime.now(), "CONCERT", "Venue", 10000, null, null, EventStatus.ACTIVE, "#FFFFFF", "😀");
        eventRepository.save(event);
        Seat seat1 = new Seat(event, "A1", SeatStatus.AVAILABLE);
        Seat seat2 = new Seat(event, "A2", SeatStatus.AVAILABLE);
        seatRepository.saveAll(List.of(seat1, seat2));

        // when
        List<Seat> seats = seatRepository.findByEventIdAndSeatNoIn(event.getId(), List.of("A1", "A2"));

        // then
        assertEquals(2, seats.size());
    }

    @Test
    @DisplayName("좌석 번호 리스트 중 이미 예약된 좌석 개수 조회")
    void countReserved() {
        // given
        Event event = new Event(null, "Test Event", LocalDateTime.now(), "CONCERT", "Venue", 10000, null, null, EventStatus.ACTIVE, "#FFFFFF", "😀");
        eventRepository.save(event);
        Seat seat1 = new Seat(event, "A1", SeatStatus.RESERVED);
        Seat seat2 = new Seat(event, "A2", SeatStatus.AVAILABLE);
        seatRepository.saveAll(List.of(seat1, seat2));

        // when
        long count = seatRepository.countReserved(event.getId(), List.of("A1", "A2"));

        // then
        assertEquals(1, count);
    }
}

package site.ticketlion.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import site.ticketlion.domain.Event;
import site.ticketlion.domain.EventStatus;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class EventRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @Test
    @DisplayName("상태별로 이벤트를 시작 시간 오름차순으로 조회")
    void findAllByStatusOrderByStartAtAsc() {
        // given
        Event activeEvent1 = new Event(null, "Active Event 1", LocalDateTime.now().plusDays(1), "CONCERT", "Venue A", 10000, null, null, EventStatus.ACTIVE, "#FFFFFF", "😀");
        Event activeEvent2 = new Event(null, "Active Event 2", LocalDateTime.now().plusDays(2), "CONCERT", "Venue B", 10000, null, null, EventStatus.ACTIVE, "#FFFFFF", "😀");
        Event inactiveEvent = new Event(null, "Inactive Event", LocalDateTime.now().plusDays(3), "CONCERT", "Venue C", 10000, null, null, EventStatus.INACTIVE, "#FFFFFF", "😀");
        eventRepository.saveAll(List.of(activeEvent2, inactiveEvent, activeEvent1));

        // when
        List<Event> activeEvents = eventRepository.findAllByStatusOrderByStartAtAsc(EventStatus.ACTIVE);

        // then
        assertEquals(2, activeEvents.size());
        assertEquals("Active Event 1", activeEvents.get(0).getTitle());
        assertEquals("Active Event 2", activeEvents.get(1).getTitle());
        assertTrue(activeEvents.get(0).getStartAt().isBefore(activeEvents.get(1).getStartAt()));
    }
}

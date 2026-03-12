package site.ticketlion.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.ticketlion.domain.Event;
import site.ticketlion.domain.EventStatus;
import site.ticketlion.repository.EventRepository;
import site.ticketlion.repository.SeatRepository;
import site.ticketlion.web.dto.request.EventCreateRequest;
import site.ticketlion.web.dto.request.EventUpdateRequest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private EventService eventService;

    @Test
    @DisplayName("이벤트 생성 성공")
    void createEvent_success() {
        // given
        EventCreateRequest request = new EventCreateRequest("Test Event", LocalDateTime.now(), "CONCERT", "Test Venue", 10000, "#FFFFFF", "😀");
        Event event = new Event(1L, request.title(), request.startAt(), request.category(), request.venue(), request.price(), null, null, EventStatus.ACTIVE, request.themeColor(), request.thumbnailEmoji());
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        // when
        Event createdEvent = eventService.createEvent(request);

        // then
        assertNotNull(createdEvent);
        assertEquals(request.title(), createdEvent.getTitle());
        verify(seatRepository).saveAll(any());
    }

    @Test
    @DisplayName("이벤트 조회 성공")
    void getEvent_success() {
        // given
        Long eventId = 1L;
        Event event = new Event(eventId, "Test Event", LocalDateTime.now(), "CONCERT", "Test Venue", 10000, null, null, EventStatus.ACTIVE, "#FFFFFF", "😀");
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // when
        Event foundEvent = eventService.getEvent(eventId);

        // then
        assertNotNull(foundEvent);
        assertEquals(eventId, foundEvent.getId());
    }

    @Test
    @DisplayName("이벤트 수정 성공")
    void updateEvent_success() {
        // given
        Long eventId = 1L;
        EventUpdateRequest request = new EventUpdateRequest("Updated Event", LocalDateTime.now().plusDays(1), "MUSICAL", "Updated Venue", 12000, "#000000", "😂");
        Event event = new Event(eventId, "Test Event", LocalDateTime.now(), "CONCERT", "Test Venue", 10000, null, null, EventStatus.ACTIVE, "#FFFFFF", "😀");
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // when
        Event updatedEvent = eventService.updateEvent(eventId, request);

        // then
        assertNotNull(updatedEvent);
        assertEquals(request.title(), updatedEvent.getTitle());
        assertEquals(request.venue(), updatedEvent.getVenue());
    }

    @Test
    @DisplayName("이벤트 삭제 성공")
    void deleteEvent_success() {
        // given
        Long eventId = 1L;

        // when
        eventService.deleteEvent(eventId);

        // then
        verify(seatRepository).deleteByEventId(eventId);
        verify(eventRepository).deleteById(eventId);
    }
}

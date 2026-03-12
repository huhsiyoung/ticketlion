package site.ticketlion.service;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.ticketlion.domain.Event;
import site.ticketlion.domain.EventStatus;
import site.ticketlion.domain.Seat;
import site.ticketlion.domain.SeatStatus;
import site.ticketlion.web.dto.request.EventCreateRequest;
import site.ticketlion.web.dto.request.EventUpdateRequest;
import site.ticketlion.web.dto.response.EventTicketingPageResponse;
import site.ticketlion.repository.EventRepository;
import site.ticketlion.repository.SeatRepository;
import site.ticketlion.web.dto.response.SeatDto;
import site.ticketlion.web.exception.EventNotFoundException;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    private final SeatRepository seatRepository;

    public List<Event> getOpenEvents() {
        return eventRepository.findAllByStatusOrderByStartAtAsc(EventStatus.ACTIVE);
    }

    @Transactional
    public Event createEvent(EventCreateRequest req) {
        // Event 엔티티는 setter가 없어서 AllArgsConstructor를 사용해 생성 (id/createdAt/updatedAt은 null로)
        Event event = new Event(
            null,
            req.title(),
            req.startAt(),
            req.category(),
            req.venue(),
            req.price(),
            null,
            null,
            EventStatus.ACTIVE,
            req.themeColor(),
            req.thumbnailEmoji()
        );

        Event saved = eventRepository.save(event);

        List<Seat> seats = generateSeats(saved);

        seatRepository.saveAll(seats);

        return saved;
    }

    @Transactional(readOnly = true)
    public Event getEvent(Long eventId) {
        return eventRepository.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("event not found"));
    }

    @Transactional
    public Event updateEvent(Long eventId, EventUpdateRequest req) {
        Event event = getEvent(eventId);

        event.update(
            req.title(),
            req.startAt(),
            req.category(),
            req.venue(),
            req.price(),
            req.themeColor(),
            req.thumbnailEmoji()
        );

        return event;
    }


    @Transactional
    public void deleteEvent(Long id) {
        seatRepository.deleteByEventId(id);

        eventRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public EventTicketingPageResponse getTicketingPage(Long eventId) {
        Event event = getEvent(eventId);

        List<Seat> seats = seatRepository.findAllByEventId(eventId);

        List<SeatDto> seatDtos = seats.stream()
            .map(s -> new SeatDto(s.getId(), s.getSeatNo(), s.getStatus()))
            .toList();

        return new EventTicketingPageResponse(
            event.getId(),
            event.getTitle(),
            event.getStartAt(),
            event.getVenue(),
            event.getPrice(),
            seatDtos
        );
    }

    private List<Seat> generateSeats(Event event) {
        List<Seat> seats = new ArrayList<>(80);

        for (char row = 'A'; row <= 'H'; row++) {
            for (int col = 1; col <= 10; col++) {
                String seatNo = row + String.valueOf(col);
                seats.add(new Seat(event, seatNo, SeatStatus.AVAILABLE));
            }
        }

        return seats;
    }

    @Transactional(readOnly = true)
    public Event findById(Long id) {
        return eventRepository.findById(id)
            .orElseThrow(() -> new EventNotFoundException(id));
    }
}

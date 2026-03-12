package site.ticketlion.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import site.ticketlion.domain.Event;
import site.ticketlion.domain.EventStatus;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findAllByStatusOrderByStartAtAsc(EventStatus status);
}

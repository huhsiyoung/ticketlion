package site.ticketlion.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.ticketlion.domain.Seat;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    /**
     * 이벤트 ID로 모든 좌석 삭제
     */
    void deleteByEventId(Long eventId);

    /**
     * 이벤트 ID로 모든 좌석 조회
     */
    List<Seat> findAllByEventId(Long eventId);

    /**
     * 이벤트 ID와 좌석 번호로 좌석 조회
     */
    Optional<Seat> findByEventIdAndSeatNo(Long eventId, String seatNo);

    /**
     * 이벤트 ID와 좌석 번호 리스트로 좌석 조회
     */
    @Query("SELECT s FROM Seat s WHERE s.event.id = :eventId AND s.seatNo IN :seatNumbers")
    List<Seat> findByEventIdAndSeatNoIn(@Param("eventId") Long eventId, @Param("seatNumbers") List<String> seatNumbers);

    /**
     * 좌석 번호 리스트 중 이미 예약된(RESERVED) 좌석 개수 조회
     */
    @Query("SELECT COUNT(s) FROM Seat s WHERE s.event.id = :eventId AND s.seatNo IN :seatNumbers AND s.status = 'RESERVED'")
    long countReserved(@Param("eventId") Long eventId, @Param("seatNumbers") List<String> seatNumbers);

    /**
     * 비관적 락(PESSIMISTIC_WRITE)을 사용하여 좌석 조회
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.event.id = :eventId AND s.seatNo IN :seatNumbers")
    List<Seat> findAndLockByEventIdAndSeatNoIn(@Param("eventId") Long eventId, @Param("seatNumbers") List<String> seatNumbers);
}

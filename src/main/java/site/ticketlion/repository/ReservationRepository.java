package site.ticketlion.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.ticketlion.domain.Reservation;
import site.ticketlion.domain.ReservationStatus;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /**
     * 예매 결과 페이지용 조회
     * Seat → Event 까지 한 번에 FETCH JOIN하여 LazyInitializationException 방지
     */
    @Query("""
        SELECT r FROM Reservation r
        JOIN FETCH r.seat s
        JOIN FETCH s.event
        WHERE r.id = :id
        """)
    Optional<Reservation> findByIdWithSeatAndEvent(@Param("id") Long id);

    /**
     * 특정 시간 이전에 생성된 PENDING 상태의 모든 예약을 조회합니다. (스케줄러용)
     * Seat 엔티티를 함께 페치 조인하여 N+1 문제를 방지합니다.
     */
    @Query("SELECT r FROM Reservation r JOIN FETCH r.seat WHERE r.status = :status AND r.reservedAt < :time")
    List<Reservation> findByStatusAndReservedAtBefore(@Param("status") ReservationStatus status, @Param("time") Instant time);

    /**
     * 동일 배치 예약 조회 — userId + eventId + reservedAt 정확히 일치 (결과 페이지 다매 표시용)
     */
    @Query("""
        SELECT r FROM Reservation r
        JOIN FETCH r.seat s
        JOIN FETCH s.event e
        WHERE r.userId = :userId AND e.id = :eventId AND r.reservedAt = :reservedAt
        """)
    List<Reservation> findBatchReservations(
        @Param("userId") UUID userId,
        @Param("eventId") Long eventId,
        @Param("reservedAt") Instant reservedAt);

    /**
     * 특정 회원 + 이벤트의 특정 상태 예약 목록 (Seat + Event FETCH JOIN)
     */
    @Query("""
        SELECT r FROM Reservation r
        JOIN FETCH r.seat s
        JOIN FETCH s.event e
        WHERE r.userId = :userId AND e.id = :eventId AND r.status = :status
        """)
    List<Reservation> findByUserIdAndEventIdAndStatus(
        @Param("userId") UUID userId,
        @Param("eventId") Long eventId,
        @Param("status") ReservationStatus status);

    /**
     * 특정 회원의 예매 내역 조회 (Seat → Event FETCH JOIN, 최신순)
     * ReservationRepository.java 에 추가하세요.
     */
    @Query("""
    SELECT r FROM Reservation r
    JOIN FETCH r.seat s
    JOIN FETCH s.event
    WHERE r.userId = :userId
    ORDER BY r.reservedAt DESC
    """)
    List<Reservation> findAllByUserIdWithSeatAndEvent(@Param("userId") UUID userId);
}

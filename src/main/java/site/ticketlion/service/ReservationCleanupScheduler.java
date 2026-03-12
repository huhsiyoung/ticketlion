package site.ticketlion.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.ticketlion.domain.Reservation;
import site.ticketlion.domain.ReservationStatus;
import site.ticketlion.domain.Seat;
import site.ticketlion.repository.ReservationRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationCleanupScheduler {

    private final ReservationRepository reservationRepository;

    private static final int EXPIRATION_MINUTES = 10;

    /**
     * 매 분마다 실행되어 만료된 예약을 정리합니다.
     */
    @Scheduled(cron = "0 * * * * *") // 매분 0초에 실행
    @Transactional
    public void cleanupExpiredReservations() {
        Instant expirationTime = Instant.now().minus(EXPIRATION_MINUTES, ChronoUnit.MINUTES);

        log.info("만료된 예약 정리 작업 시작. 기준 시간: {}", expirationTime);

        // 1. 만료된 예약 조회 (10분 이상 경과, PENDING 상태)
        List<Reservation> expiredReservations = reservationRepository.findByStatusAndReservedAtBefore(
            ReservationStatus.PENDING, expirationTime);

        if (expiredReservations.isEmpty()) {
            log.info("만료된 예약이 없습니다.");
            return;
        }

        log.info("{}개의 만료된 예약을 발견했습니다.", expiredReservations.size());

        // 2. 좌석 상태를 AVAILABLE로 변경 및 예약 상태를 CANCELLED로 변경
        int cancelledCount = 0;

        for (Reservation reservation : expiredReservations) {
            Seat seat = reservation.getSeat();

            if (seat != null) {
                log.info("좌석 {} (ID: {})의 상태를 AVAILABLE로 변경합니다.", seat.getSeatNo(), seat.getId());
                seat.release();
            }

            reservation.cancel(); // Reservation의 상태를 CANCELLED로 변경하는 메서드

            cancelledCount++;
        }

        log.info("만료된 예약 {}개의 상태를 CANCELLED로 변경했습니다.", cancelledCount);
    }
}

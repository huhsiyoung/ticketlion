package site.ticketlion.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.ticketlion.domain.Reservation;
import site.ticketlion.domain.ReservationStatus;
import site.ticketlion.domain.Seat;
import site.ticketlion.domain.SeatStatus;
import site.ticketlion.web.dto.response.MyReservationResponse;
import site.ticketlion.web.dto.response.SeatDto;
import site.ticketlion.repository.ReservationRepository;
import site.ticketlion.repository.SeatRepository;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ObjectMapper objectMapper;

    private final StringRedisTemplate stringRedisTemplate;

    private final SeatRepository seatRepository;

    private final DefaultRedisScript<Long> holdSeatsScript;

    private final ReservationRepository reservationRepository;

    /**
     * 이 이벤트에서 사용자가 현재 Redis에 홀드한 좌석 수
     */
    public int getHeldSeatCount(Long eventId, UUID memberId) {
        Set<String> keys = stringRedisTemplate.keys("seat:hold:" + eventId + ":*");
        if (keys == null || keys.isEmpty()) return 0;
        return (int) keys.stream()
            .filter(k -> memberId.toString().equals(stringRedisTemplate.opsForValue().get(k)))
            .count();
    }

    /**
     * 좌석 선점 (Redis HOLD)
     */
    @Transactional(readOnly = true)
    public void holdSeats(Long eventId, List<String> seatNumbers, UUID memberId) {

        // 0) 1인 2매 제한: 기존 홀드 + 신규 홀드 ≤ 2
        int existingHoldCount = getHeldSeatCount(eventId, memberId);
        int remaining = 2 - existingHoldCount;
        if (seatNumbers.size() > remaining) {
            if (existingHoldCount > 0) {
                throw new IllegalStateException(
                    "현재 결제 대기 중인 좌석 " + existingHoldCount + "석이 있어 추가로 " + remaining
                        + "석만 선택할 수 있습니다. 다른 좌석으로 변경하려면 기존 결제 대기를 취소한 뒤 다시 선택해주세요."
                );
            } else {
                throw new IllegalStateException("1인 최대 2매까지 예매 가능합니다.");
            }
        }

        // 1) RDB 검사: 하나라도 RESERVED면 실패
        long reservedCount = seatRepository.countReserved(eventId, seatNumbers);

        if (reservedCount > 0) {
            throw new IllegalStateException("이미 예매 완료된 좌석이 포함되어 있습니다.");
        }

        // 2) Redis HOLD (원자적)
        List<String> keys = seatNumbers.stream()
            .map(seatNo -> "seat:hold:" + eventId + ":" + seatNo)
            .toList();

        long ttlSeconds = 600; // 10분

        Long ok = stringRedisTemplate.execute(
            holdSeatsScript,
            keys,
            memberId.toString(),
            String.valueOf(ttlSeconds)
        );

        if (ok == null || ok == 0L) {
            throw new IllegalStateException("다른 사용자가 먼저 선택했습니다(홀드 실패).");
        }
    }

    /**
     * 좌석 예매 - Reservation 생성 및 Seat 상태 변경 (Redis 삭제는 cleanupHeldSeats에서 수행)
     * reservation-process.html의 '결제 확인' 단계(Step 1)에서 호출
     */
    @Transactional
    public Long reserveSeats(Long eventId, List<String> seatNumbers, UUID memberId) {

        // 1) Redis에서 홀드 확인
        for (String seatNo : seatNumbers) {
            String key = "seat:hold:" + eventId + ":" + seatNo;
            String heldBy = stringRedisTemplate.opsForValue().get(key);

            if (heldBy == null || !heldBy.equals(memberId.toString())) {
                throw new IllegalStateException("선점 시간이 만료되었거나 권한이 없습니다.");
            }
        }

        // 2) RDB에서 좌석 조회
        List<Seat> seats = seatRepository.findByEventIdAndSeatNoIn(eventId, seatNumbers);

        if (seats.size() != seatNumbers.size()) {
            throw new IllegalStateException("존재하지 않는 좌석이 포함되어 있습니다.");
        }

        // 3) 좌석 상태 변경 (AVAILABLE → RESERVED)
        for (Seat seat : seats) {
            seat.reserve();
        }

        // 4) Reservation 생성 (상태: PENDING)
        List<Reservation> reservations = seats.stream()
            .map(seat -> new Reservation(
                memberId,
                Instant.now(),
                seat
            ))
            .toList();

        List<Reservation> savedReservations = reservationRepository.saveAll(reservations);

        // Redis 삭제는 하지 않음 — cleanupHeldSeats(Step 2)에서 처리
        return savedReservations.get(0).getId();
    }

    /**
     * 좌석 예매 (비관적 락 사용)
     */
    @Transactional
    public Long reserveSeatsWithPessimisticLock(Long eventId, List<String> seatNumbers, UUID memberId) {
        // 1. 비관적 락을 사용하여 좌석 조회 및 잠금
        // 이 시점에서 다른 트랜잭션은 이 좌석들에 대한 쓰기 접근이 차단되고 대기합니다.
        List<Seat> seats = seatRepository.findAndLockByEventIdAndSeatNoIn(eventId, seatNumbers);

        // 2. 요청한 좌석이 모두 존재하는지 확인
        if (seats.size() != seatNumbers.size()) {
            throw new IllegalStateException("존재하지 않는 좌석이 포함되어 있습니다.");
        }

        // 3. 이미 예약된 좌석이 있는지 확인 (락을 확보했으므로 이 검사는 안전합니다)
        for (Seat seat : seats) {
            if (seat.getStatus() == SeatStatus.RESERVED) {
                // 트랜잭션이 롤백되면서 락이 해제됩니다.
                throw new IllegalStateException("이미 예매 완료된 좌석이 포함되어 있습니다: " + seat.getSeatNo());
            }
        }

        // 4. 좌석 상태 변경 (AVAILABLE → RESERVED)
        for (Seat seat : seats) {
            seat.reserve();
        }

        // 5. Reservation 생성 (동일 배치는 같은 reservedAt 사용 — 결과 페이지 일괄 조회에 활용)
        Instant now = Instant.now();

        List<Reservation> reservations = seats.stream()
            .map(seat -> new Reservation(memberId, now, seat))
            .toList();

        reservationRepository.saveAll(reservations);

        // 6. 트랜잭션이 커밋되면서 락이 최종 해제되고, 변경사항이 DB에 반영됩니다.
        return reservations.get(0).getId();
    }

    /**
     * 예약 확정 및 Redis 홀드 삭제
     * reservation-process.html의 '좌석 배정' 단계(Step 2)에서 호출
     * 동일 사용자 + 이벤트의 PENDING 예약 전체를 CONFIRMED로 처리 (2매 동시 결제 대응)
     */
    @Transactional
    public void cleanupHeldSeats(Long reservationId, UUID memberId) {

        Reservation primary = reservationRepository.findByIdWithSeatAndEvent(reservationId)
            .orElseThrow(() -> new IllegalStateException("예매 정보를 찾을 수 없습니다."));

        if (!primary.getUserId().equals(memberId)) {
            throw new IllegalStateException("예매자 정보가 일치하지 않습니다.");
        }

        Long eventId = primary.getSeat().getEvent().getId();

        // 동일 사용자 + 이벤트의 PENDING 예약 전체 확정 (1매/2매 모두 처리)
        List<Reservation> pending = reservationRepository.findByUserIdAndEventIdAndStatus(
            memberId, eventId, ReservationStatus.PENDING);

        for (Reservation r : pending) {
            r.confirm();
            stringRedisTemplate.delete("seat:hold:" + eventId + ":" + r.getSeat().getSeatNo());
        }
    }

    /**
     * 사용자가 선점한 좌석 조회
     */
    @Transactional(readOnly = true)
    public List<SeatDto> getHeldSeats(Long eventId, UUID memberId) {
        Set<String> keys = stringRedisTemplate.keys("seat:hold:" + eventId + ":*");

        List<SeatDto> heldSeats = new ArrayList<>();

        if (keys != null) {
            for (String key : keys) {
                String heldBy = stringRedisTemplate.opsForValue().get(key);

                if (memberId.toString().equals(heldBy)) {
                    String seatNo = key.substring(key.lastIndexOf(":") + 1);

                    Seat seat = seatRepository.findByEventIdAndSeatNo(eventId, seatNo)
                        .orElseThrow(() -> new IllegalStateException("좌석을 찾을 수 없습니다."));

                    heldSeats.add(new SeatDto(
                        seat.getId(),
                        seat.getSeatNo(),
                        SeatStatus.AVAILABLE
                    ));
                }
            }
        }

        return heldSeats;
    }

    /**
     * 좌석 ID로 좌석 번호 조회
     */
    @Transactional(readOnly = true)
    public String getSeatNumberById(Long seatId) {
        return seatRepository.findById(seatId)
            .map(Seat::getSeatNo)
            .orElseThrow(() -> new IllegalStateException("좌석을 찾을 수 없습니다."));
    }

    /**
     * 예매 정보 조회 (결과 페이지용 - Seat, Event 까지 FETCH JOIN)
     */
    @Transactional(readOnly = true)
    public Reservation getReservationResult(Long reservationId) {
        return reservationRepository.findByIdWithSeatAndEvent(reservationId)
            .orElseThrow(() -> new IllegalStateException("예매 정보를 찾을 수 없습니다."));
    }

    /**
     * 동일 배치 예약 전체 조회 (결과 페이지 다매 좌석 표시용)
     */
    @Transactional(readOnly = true)
    public List<Reservation> getBatchReservations(UUID userId, Long eventId, Instant reservedAt) {
        return reservationRepository.findBatchReservations(userId, eventId, reservedAt);
    }

    /**
     * 내 예매 내역 조회
     * ReservationService.java 에 추가하세요.
     */
    @Transactional(readOnly = true)
    public List<MyReservationResponse> getMyReservations(UUID memberId) {
        return reservationRepository.findAllByUserIdWithSeatAndEvent(memberId)
            .stream()
            .map(MyReservationResponse::from)
            .toList();
    }

    /**
     * 사용 가능한 좌석 조회 (Redis 홀드 상태 반영)
     */
    @Transactional(readOnly = true)
    public String getAvailableSeat(Long eventId) {
        List<Seat> seats = seatRepository.findAllByEventId(eventId);

        Set<String> keys = stringRedisTemplate.keys("seat:hold:" + eventId + ":*");

        Set<String> heldSeatNos = keys != null
            ? keys.stream()
            .map(key -> key.substring(key.lastIndexOf(":") + 1))
            .collect(Collectors.toSet())
            : Set.of();

        List<SeatDto> result = new ArrayList<>();

        for (Seat seat : seats) {
            SeatStatus status = seat.getStatus();

            if (heldSeatNos.contains(seat.getSeatNo())) {
                status = SeatStatus.RESERVED;
            }

            result.add(new SeatDto(
                seat.getId(),
                seat.getSeatNo(),
                status
            ));
        }

        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            throw new RuntimeException("JSON 변환 실패", e);
        }
    }

    /**
     * 예매 취소 (PENDING / CONFIRMED → CANCELLED)
     */
    @Transactional
    public void cancelReservation(Long reservationId, UUID memberId) {
        Reservation reservation = reservationRepository.findByIdWithSeatAndEvent(reservationId)
            .orElseThrow(() -> new IllegalStateException("예매 정보를 찾을 수 없습니다."));

        if (!reservation.getUserId().equals(memberId)) {
            throw new IllegalStateException("예매자 정보가 일치하지 않습니다.");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 예매입니다.");
        }

        // Redis hold 키 삭제 (취소 시 남아있는 홀드 제거 — TTL 만료 전 수동 취소 대응)
        Long eventId = reservation.getSeat().getEvent().getId();
        stringRedisTemplate.delete("seat:hold:" + eventId + ":" + reservation.getSeat().getSeatNo());

        reservation.getSeat().release();
        reservation.cancel();
    }
}

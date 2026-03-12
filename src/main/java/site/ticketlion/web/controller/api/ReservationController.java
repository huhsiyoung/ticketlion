package site.ticketlion.web.controller.api;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import site.ticketlion.domain.MemberUserDetails;
import site.ticketlion.domain.Payment;
import site.ticketlion.domain.Reservation;
import site.ticketlion.service.PaymentService;
import site.ticketlion.service.ReservationService;
import site.ticketlion.web.dto.request.PaymentRequest;
import site.ticketlion.web.dto.request.SeatHoldRequest;
import site.ticketlion.web.dto.response.MyReservationResponse;
import site.ticketlion.web.dto.response.PaymentResponse;
import site.ticketlion.web.dto.response.SeatHoldResponse;

@Controller
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final PaymentService paymentService;

    /**
     * IllegalStateException → 400 Bad Request
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseBody
    public ResponseEntity<String> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    /**
     * 좌석 선점 API
     */
    @PostMapping("/events/{eventId}/holds")
    @ResponseBody
    public SeatHoldResponse holdSeats(
            @PathVariable Long eventId,
            @RequestBody SeatHoldRequest request,
            Authentication authentication
    ) {
        MemberUserDetails userDetails = (MemberUserDetails) authentication.getPrincipal();
        UUID memberId = userDetails.getMemberId();

        List<String> seatNumbers = request.getSeatIds().stream()
                .map(seatId -> reservationService.getSeatNumberById(seatId))
                .toList();

        reservationService.holdSeats(eventId, seatNumbers, memberId);

        return new SeatHoldResponse(
                true,
                "좌석 선점 성공",
                seatNumbers
        );
    }

    /**
     * 좌석 상태 조회 API (Redis 홀드 상태 반영)
     */
    @GetMapping("/api/events/{eventId}/seats")
    @ResponseBody
    public String getSeats(@PathVariable Long eventId) {
        return reservationService.getAvailableSeat(eventId);
    }

    /**
     * 예매 처리 중 페이지 (reservation-process.html)
     * - 실제 처리는 JS에서 단계별 API 호출로 수행
     */
    @GetMapping("/reservation-process")
    public String reservationProcessPage() {
        return "reservation-process";
    }

    /**
     * [Step 1] 결제 처리 API
     * - Redis 홀드 확인 → RDB에 Reservation 생성 → Payment(0원) 처리
     * - 반환: reservationId (process 페이지에서 사용)
     * reservation-process.html의 '결제 확인' 단계에서 호출
     */
    @PostMapping("/reservations/payments")
    @ResponseBody
    public PaymentResponse payment(
            @RequestBody PaymentRequest request,
            Authentication authentication
    ) {
        MemberUserDetails userDetails = (MemberUserDetails) authentication.getPrincipal();
        UUID memberId = userDetails.getMemberId();

        // 1) Redis 홀드 확인 + RDB에 Reservation 생성 (Redis 삭제는 아직 안 함)
        Long reservationId = reservationService.reserveSeatsWithPessimisticLock(
                request.getEventId(),
                request.getSeatNumbers(),
                memberId
        );

        // 2) 0원 무료 결제 처리 (Payment 이력 DB 저장, Reservation → PAID)
        UUID idempotencyKey = UUID.randomUUID();

        Payment payment = paymentService.pay(reservationId, memberId, idempotencyKey);

        return new PaymentResponse(
                payment.getId(),
                reservationId,
                payment.getAmount(),
                payment.getStatus().name()
        );
    }

    /**
     * [Step 2] 좌석 최종 배정 API
     * - Redis에서 홀드 정보 삭제 (좌석 배정 확정)
     * - reservation-process.html의 '좌석 배정' 단계에서 호출
     */
    @PostMapping("/reservations/{reservationId}/assign")
    @ResponseBody
    public void assignSeats(
            @PathVariable Long reservationId,
            Authentication authentication
    ) {
        MemberUserDetails userDetails = (MemberUserDetails) authentication.getPrincipal();
        UUID memberId = userDetails.getMemberId();

        reservationService.cleanupHeldSeats(reservationId, memberId);
    }

    /**
     * 예매 결과 페이지 (reservation-result.html)
     */
    @GetMapping("/reservations/{reservationId}/results")
    public String getReservationResult(
            @PathVariable Long reservationId,
            Model model,
            Authentication authentication
    ) {
        MemberUserDetails userDetails = (MemberUserDetails) authentication.getPrincipal();
        UUID userId = userDetails.getMemberId();

        Reservation primary = reservationService.getReservationResult(reservationId);

        List<Reservation> batchReservations = reservationService.getBatchReservations(
                userId,
                primary.getSeat().getEvent().getId(),
                primary.getReservedAt()
        );

        model.addAttribute("reservation", primary);
        model.addAttribute("reservations", batchReservations.isEmpty()
                ? List.of(primary) : batchReservations);

        return "reservation-result";
    }

    /**
     * 내 예매 내역 조회
     * GET /api/reservations/my
     */
    @GetMapping("/api/reservations/my")
    @ResponseBody
    public ResponseEntity<List<MyReservationResponse>> getMyReservations(
            @AuthenticationPrincipal MemberUserDetails userDetails) {

        List<MyReservationResponse> result = reservationService
                .getMyReservations(userDetails.getMemberId());

        return ResponseEntity.ok(result);
    }

    /**
     * 예매 취소
     * PATCH /api/reservations/{reservationId}/cancel
     */
    @PatchMapping("/api/reservations/{reservationId}/cancel")
    @ResponseBody
    public ResponseEntity<Void> cancelReservation(
            @PathVariable Long reservationId,
            @AuthenticationPrincipal MemberUserDetails userDetails) {

        reservationService.cancelReservation(reservationId, userDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}

package site.ticketlion.web.controller.page;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import site.ticketlion.domain.MemberUserDetails;
import site.ticketlion.repository.SeatRepository;
import site.ticketlion.service.EventService;
import site.ticketlion.service.MemberService;
import site.ticketlion.service.ReservationService;

@Controller
@RequiredArgsConstructor
public class PageController {

    private static final Integer MAX_SELECT = 2;

    private final EventService eventService;
    private final MemberService memberService;
    private final ReservationService reservationService;

    private final SeatRepository seatRepository;

    @GetMapping("/")
    public String getEvent(Model model) {
        return "redirect:/events";
    }

    @GetMapping("/process")
    public String process() {
        return "reservation-process";
    }

    @GetMapping("/events")
    public String event(Model model) {
        model.addAttribute("events", eventService.getOpenEvents());

        return "event";
    }

    @GetMapping("/events/{eventId}/seats")
    public String selectSeat(@PathVariable Long eventId, Model model, Authentication authentication) {
        int pendingCount = 0;
        if (authentication != null && authentication.isAuthenticated()) {
            MemberUserDetails userDetails = (MemberUserDetails) authentication.getPrincipal();
            pendingCount = reservationService.getHeldSeatCount(eventId, userDetails.getMemberId());
        }
        int maxSelect = Math.max(0, MAX_SELECT - pendingCount);

        model.addAttribute("event", eventService.getEvent(eventId));
        model.addAttribute("seats", reservationService.getAvailableSeat(eventId));
        model.addAttribute("maxSelect", maxSelect);
        model.addAttribute("pendingCount", pendingCount);

        return "seat-selection";
    }

    /**
     * 예매 페이지 (reservation-payment.html)
     * - Redis에서 선점된 좌석 정보 조회
     */
    @GetMapping("/events/{eventId}/reservations")
    public String getReservationPayment(
        @PathVariable Long eventId,
        Model model,
        Authentication authentication
    ) {
        MemberUserDetails userDetails = (MemberUserDetails) authentication.getPrincipal();
        UUID memberId = userDetails.getMemberId();

        var event = eventService.getEvent(eventId);
        var heldSeats = reservationService.getHeldSeats(eventId, memberId);

        model.addAttribute("event", event);
        model.addAttribute("seats", heldSeats);
        model.addAttribute("totalPrice", heldSeats.size() * event.getPrice());

        return "reservation-payment";
    }

    @GetMapping("/reservation-details")
    public String getReservationDetails(Model model,
        @AuthenticationPrincipal MemberUserDetails userDetails /* 또는 실제 인증 객체 */) {
        model.addAttribute("member", memberService.getMember(userDetails.getMemberId()));

        return "reservation-details";
    }
}

package site.ticketlion.web.controller.page;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import site.ticketlion.config.SecurityConfigDev;
import site.ticketlion.domain.Event;
import site.ticketlion.domain.EventStatus;
import site.ticketlion.domain.Member;
import site.ticketlion.domain.MemberRole;
import site.ticketlion.domain.MemberUserDetails;
import site.ticketlion.domain.SeatStatus;
import site.ticketlion.repository.SeatRepository;
import site.ticketlion.service.EventService;
import site.ticketlion.service.MemberService;
import site.ticketlion.service.ReservationService;
import site.ticketlion.web.dto.response.MemberResponse;
import site.ticketlion.web.dto.response.SeatDto;

@WebMvcTest(PageController.class)
@Import(SecurityConfigDev.class)
@ActiveProfiles("dev")
class PageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private ReservationService reservationService;

    @MockitoBean
    private SeatRepository seatRepository;

    private MemberUserDetails userDetails;
    private Authentication auth;
    private UUID memberId;

    @BeforeEach
    void setUp() {
        memberId = UUID.randomUUID();
        Member member = new Member(memberId, "테스트유저", "test@example.com", "hashed", "010-0000-0000",
            MemberRole.USER, null, null);
        userDetails = new MemberUserDetails(member);
        auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    private Event sampleEvent() {
        return new Event(1L, "공연A", LocalDateTime.now(), "콘서트", "서울", 50000,
            null, null, EventStatus.ACTIVE, "#FF0000", "🎵");
    }

    // ---------------------------------------------------------------
    // GET /
    // ---------------------------------------------------------------

    @Test
    @DisplayName("루트 경로 요청 시 /events로 리다이렉트")
    void root_redirectsToEvents() throws Exception {
        mockMvc.perform(get("/").with(authentication(auth)))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/events"));
    }

    @Test
    @DisplayName("루트 경로 - 인증 없이 접근 시 로그인으로 리다이렉트")
    void root_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().is3xxRedirection());
    }

    // ---------------------------------------------------------------
    // GET /process
    // ---------------------------------------------------------------

    @Test
    @DisplayName("예약 진행 페이지 요청 시 reservation-process 뷰 반환")
    void process_returnsView() throws Exception {
        mockMvc.perform(get("/process").with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(view().name("reservation-process"));
    }

    @Test
    @DisplayName("예약 진행 페이지 - 인증 없이 접근 시 리다이렉트")
    void process_unauthenticated_redirects() throws Exception {
        mockMvc.perform(get("/process"))
            .andExpect(status().is3xxRedirection());
    }

    // ---------------------------------------------------------------
    // GET /events
    // ---------------------------------------------------------------

    @Test
    @DisplayName("이벤트 목록 페이지 요청 시 event 뷰와 events 모델 반환")
    void events_returnsViewWithEvents() throws Exception {
        when(eventService.getOpenEvents()).thenReturn(List.of(sampleEvent()));

        mockMvc.perform(get("/events").with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(view().name("event"))
            .andExpect(model().attributeExists("events"));

        verify(eventService).getOpenEvents();
    }

    @Test
    @DisplayName("이벤트 목록 페이지 - 인증 없이 접근 시 리다이렉트")
    void events_unauthenticated_redirects() throws Exception {
        mockMvc.perform(get("/events"))
            .andExpect(status().is3xxRedirection());
    }

    // ---------------------------------------------------------------
    // GET /events/{eventId}/seats
    // ---------------------------------------------------------------

    @Test
    @DisplayName("좌석 선택 페이지 - 선점 없을 때 maxSelect=2, pendingCount=0")
    void selectSeat_noPending_maxSelect2() throws Exception {
        when(eventService.getEvent(1L)).thenReturn(sampleEvent());
        when(reservationService.getAvailableSeat(1L)).thenReturn("[]");
        when(reservationService.getHeldSeatCount(1L, memberId)).thenReturn(0);

        mockMvc.perform(get("/events/1/seats").with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(view().name("seat-selection"))
            .andExpect(model().attribute("maxSelect", 2))
            .andExpect(model().attribute("pendingCount", 0));
    }

    @Test
    @DisplayName("좌석 선택 페이지 - 1석 선점 시 maxSelect=1")
    void selectSeat_withOnePending_maxSelect1() throws Exception {
        when(eventService.getEvent(1L)).thenReturn(sampleEvent());
        when(reservationService.getAvailableSeat(1L)).thenReturn("[]");
        when(reservationService.getHeldSeatCount(1L, memberId)).thenReturn(1);

        mockMvc.perform(get("/events/1/seats").with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(view().name("seat-selection"))
            .andExpect(model().attribute("maxSelect", 1))
            .andExpect(model().attribute("pendingCount", 1));
    }

    @Test
    @DisplayName("좌석 선택 페이지 - 2석 선점 시 maxSelect=0")
    void selectSeat_withTwoPending_maxSelect0() throws Exception {
        when(eventService.getEvent(1L)).thenReturn(sampleEvent());
        when(reservationService.getAvailableSeat(1L)).thenReturn("[]");
        when(reservationService.getHeldSeatCount(1L, memberId)).thenReturn(2);

        mockMvc.perform(get("/events/1/seats").with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(view().name("seat-selection"))
            .andExpect(model().attribute("maxSelect", 0))
            .andExpect(model().attribute("pendingCount", 2));
    }

    @Test
    @DisplayName("좌석 선택 페이지 - 인증 없이 접근 시 리다이렉트")
    void selectSeat_unauthenticated_redirects() throws Exception {
        mockMvc.perform(get("/events/1/seats"))
            .andExpect(status().is3xxRedirection());
    }

    // ---------------------------------------------------------------
    // GET /events/{eventId}/reservations
    // ---------------------------------------------------------------

    @Test
    @DisplayName("예매 결제 페이지 - 선점 좌석 및 총금액 모델에 포함")
    void getReservationPayment_returnsViewWithModel() throws Exception {
        Event event = sampleEvent();
        List<SeatDto> heldSeats = List.of(
            new SeatDto(1L, "A1", SeatStatus.AVAILABLE),
            new SeatDto(2L, "A2", SeatStatus.AVAILABLE)
        );

        when(eventService.getEvent(1L)).thenReturn(event);
        when(reservationService.getHeldSeats(1L, memberId)).thenReturn(heldSeats);

        mockMvc.perform(get("/events/1/reservations").with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(view().name("reservation-payment"))
            .andExpect(model().attribute("event", event))
            .andExpect(model().attribute("seats", heldSeats))
            .andExpect(model().attribute("totalPrice", 100000));
    }

    @Test
    @DisplayName("예매 결제 페이지 - 인증 없이 접근 시 리다이렉트")
    void getReservationPayment_unauthenticated_redirects() throws Exception {
        mockMvc.perform(get("/events/1/reservations"))
            .andExpect(status().is3xxRedirection());
    }

    // ---------------------------------------------------------------
    // GET /reservation-details
    // ---------------------------------------------------------------

    @Test
    @DisplayName("예매 상세 페이지 - 회원 정보 모델에 포함")
    void getReservationDetails_returnsViewWithMember() throws Exception {
        MemberResponse memberResponse = new MemberResponse(memberId, "테스트유저", "test@example.com",
            "010-0000-0000", MemberRole.USER, null);
        when(memberService.getMember(memberId)).thenReturn(memberResponse);

        mockMvc.perform(get("/reservation-details").with(authentication(auth)))
            .andExpect(status().isOk())
            .andExpect(view().name("reservation-details"))
            .andExpect(model().attribute("member", memberResponse));

        verify(memberService).getMember(memberId);
    }

    @Test
    @DisplayName("예매 상세 페이지 - 인증 없이 접근 시 리다이렉트")
    void getReservationDetails_unauthenticated_redirects() throws Exception {
        mockMvc.perform(get("/reservation-details"))
            .andExpect(status().is3xxRedirection());
    }
}
package site.ticketlion.web.controller.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import site.ticketlion.domain.Member;
import site.ticketlion.domain.MemberRole;
import site.ticketlion.domain.MemberUserDetails;
import site.ticketlion.domain.PaymentStatus;
import site.ticketlion.domain.ReservationStatus;
import site.ticketlion.service.AuthService;
import site.ticketlion.service.PaymentService;
import site.ticketlion.service.ReservationService;
import site.ticketlion.web.dto.request.PaymentRequest;
import site.ticketlion.web.dto.request.SeatHoldRequest;
import site.ticketlion.web.dto.response.MyReservationResponse;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(ReservationController.class)
class ReservationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    ReservationService reservationService;

    @MockitoBean
    PaymentService paymentService;

    @MockitoBean
    AuthService authService;

    UUID memberId;
    MemberUserDetails userDetails;
    Authentication auth;

    @BeforeEach
    void setUp() {
        memberId = UUID.randomUUID();
        Member member = new Member(memberId, "Test User", "test@example.com", "password",
                "01012345678", MemberRole.USER, LocalDateTime.now(), LocalDateTime.now());
        userDetails = new MemberUserDetails(member);
        auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    // ──────────────────────────────────────────────
    // POST /events/{eventId}/holds - 좌석 선점
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("POST /events/{eventId}/holds - 좌석 선점")
    class HoldSeats {

        @Test
        @DisplayName("정상 요청 시 SeatHoldResponse 반환")
        void holdSeats_success() throws Exception {
            Long eventId = 1L;
            SeatHoldRequest request = new SeatHoldRequest(List.of(101L, 102L));

            given(reservationService.getSeatNumberById(101L)).willReturn("A1");
            given(reservationService.getSeatNumberById(102L)).willReturn("A2");
            willDoNothing().given(reservationService).holdSeats(eq(eventId), anyList(), eq(memberId));

            mockMvc.perform(post("/events/{eventId}/holds", eventId)
                            .with(authentication(auth))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("좌석 선점 성공"))
                    .andExpect(jsonPath("$.seatNumbers[0]").value("A1"))
                    .andExpect(jsonPath("$.seatNumbers[1]").value("A2"));
        }

        @Test
        @DisplayName("1인 2매 초과 시 400 반환")
        void holdSeats_exceedLimit() throws Exception {
            Long eventId = 1L;
            SeatHoldRequest request = new SeatHoldRequest(List.of(101L, 102L, 103L));

            given(reservationService.getSeatNumberById(anyLong())).willReturn("A1");
            willThrow(new IllegalStateException("1인 최대 2매까지 예매 가능합니다."))
                    .given(reservationService).holdSeats(eq(eventId), anyList(), eq(memberId));

            mockMvc.perform(post("/events/{eventId}/holds", eventId)
                            .with(authentication(auth))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("이미 예매된 좌석 포함 시 400 반환")
        void holdSeats_alreadyReserved() throws Exception {
            Long eventId = 1L;
            SeatHoldRequest request = new SeatHoldRequest(List.of(101L));

            given(reservationService.getSeatNumberById(101L)).willReturn("A1");
            willThrow(new IllegalStateException("이미 예매 완료된 좌석이 포함되어 있습니다."))
                    .given(reservationService).holdSeats(eq(eventId), anyList(), eq(memberId));

            mockMvc.perform(post("/events/{eventId}/holds", eventId)
                            .with(authentication(auth))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ──────────────────────────────────────────────
    // GET /api/events/{eventId}/seats - 좌석 상태 조회
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/events/{eventId}/seats - 좌석 상태 조회")
    class GetSeats {

        @Test
        @DisplayName("좌석 목록 JSON 문자열 반환")
        void getSeats_success() throws Exception {
            Long eventId = 1L;
            String jsonSeats = "[{\"id\":1,\"seatNo\":\"A1\",\"status\":\"AVAILABLE\"}]";

            given(reservationService.getAvailableSeat(eventId)).willReturn(jsonSeats);

            mockMvc.perform(get("/api/events/{eventId}/seats", eventId)
                            .with(authentication(auth)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(jsonSeats));
        }
    }

    // ──────────────────────────────────────────────
    // POST /reservations/payments - [Step 1] 결제 처리
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("POST /reservations/payments - 결제 처리")
    class Payment {

        @Test
        @DisplayName("정상 결제 시 PaymentResponse 반환")
        void payment_success() throws Exception {
            PaymentRequest request = new PaymentRequest(1L, List.of("A1", "A2"), null, null);
            Long reservationId = 10L;

            site.ticketlion.domain.Payment payment = site.ticketlion.domain.Payment.builder()
                    .id(99L)
                    .reservationId(reservationId)
                    .userId(memberId)
                    .amount(0L)
                    .status(PaymentStatus.SUCCESS)
                    .build();

            given(reservationService.reserveSeatsWithPessimisticLock(
                    eq(1L), anyList(), eq(memberId))).willReturn(reservationId);
            given(paymentService.pay(eq(reservationId), eq(memberId), any(UUID.class)))
                    .willReturn(payment);

            mockMvc.perform(post("/reservations/payments")
                            .with(authentication(auth))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reservationId").value(reservationId))
                    .andExpect(jsonPath("$.amount").value(0))
                    .andExpect(jsonPath("$.status").value("SUCCESS"));
        }

        @Test
        @DisplayName("홀드 만료 시 400 반환")
        void payment_holdExpired() throws Exception {
            PaymentRequest request = new PaymentRequest(1L, List.of("A1"), null, null);

            willThrow(new IllegalStateException("선점 시간이 만료되었거나 권한이 없습니다."))
                    .given(reservationService).reserveSeatsWithPessimisticLock(anyLong(), anyList(), any());

            mockMvc.perform(post("/reservations/payments")
                            .with(authentication(auth))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ──────────────────────────────────────────────
    // POST /reservations/{reservationId}/assign - [Step 2] 좌석 배정
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("POST /reservations/{reservationId}/assign - 좌석 배정 확정")
    class AssignSeats {

        @Test
        @DisplayName("정상 요청 시 200 반환")
        void assignSeats_success() throws Exception {
            Long reservationId = 10L;
            willDoNothing().given(reservationService).cleanupHeldSeats(reservationId, memberId);

            mockMvc.perform(post("/reservations/{reservationId}/assign", reservationId)
                            .with(authentication(auth))
                            .with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("예매자 불일치 시 400 반환")
        void assignSeats_unauthorized() throws Exception {
            Long reservationId = 10L;
            willThrow(new IllegalStateException("예매자 정보가 일치하지 않습니다."))
                    .given(reservationService).cleanupHeldSeats(eq(reservationId), any());

            mockMvc.perform(post("/reservations/{reservationId}/assign", reservationId)
                            .with(authentication(auth))
                            .with(csrf()))
                    .andExpect(status().isBadRequest());
        }
    }

    // ──────────────────────────────────────────────
    // GET /api/reservations/my - 내 예매 내역
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/reservations/my - 내 예매 내역 조회")
    class GetMyReservations {

        @Test
        @DisplayName("예매 내역 리스트 반환")
        void getMyReservations_success() throws Exception {
            List<MyReservationResponse> response = List.of(
                    new MyReservationResponse(1L, "테스트 공연", "서울", LocalDateTime.of(2025, 1, 1, 19, 0),
                            "A1", Instant.now(), ReservationStatus.CONFIRMED)
            );

            given(reservationService.getMyReservations(memberId)).willReturn(response);

            mockMvc.perform(get("/api/reservations/my")
                            .with(authentication(auth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1L))
                    .andExpect(jsonPath("$[0].eventTitle").value("테스트 공연"));
        }

        @Test
        @DisplayName("예매 없을 시 빈 리스트 반환")
        void getMyReservations_empty() throws Exception {
            given(reservationService.getMyReservations(memberId)).willReturn(List.of());

            mockMvc.perform(get("/api/reservations/my")
                            .with(authentication(auth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    // ──────────────────────────────────────────────
    // PATCH /api/reservations/{reservationId}/cancel - 예매 취소
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("PATCH /api/reservations/{reservationId}/cancel - 예매 취소")
    class CancelReservation {

        @Test
        @DisplayName("정상 취소 시 204 No Content 반환")
        void cancelReservation_success() throws Exception {
            Long reservationId = 10L;
            willDoNothing().given(reservationService).cancelReservation(reservationId, memberId);

            mockMvc.perform(patch("/api/reservations/{reservationId}/cancel", reservationId)
                            .with(authentication(auth))
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("이미 취소된 예매 시 400 반환")
        void cancelReservation_alreadyCancelled() throws Exception {
            Long reservationId = 10L;
            willThrow(new IllegalStateException("이미 취소된 예매입니다."))
                    .given(reservationService).cancelReservation(eq(reservationId), any());

            mockMvc.perform(patch("/api/reservations/{reservationId}/cancel", reservationId)
                            .with(authentication(auth))
                            .with(csrf()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("예매자 불일치 시 400 반환")
        void cancelReservation_unauthorized() throws Exception {
            Long reservationId = 10L;
            willThrow(new IllegalStateException("예매자 정보가 일치하지 않습니다."))
                    .given(reservationService).cancelReservation(eq(reservationId), any());

            mockMvc.perform(patch("/api/reservations/{reservationId}/cancel", reservationId)
                            .with(authentication(auth))
                            .with(csrf()))
                    .andExpect(status().isBadRequest());
        }
    }
}

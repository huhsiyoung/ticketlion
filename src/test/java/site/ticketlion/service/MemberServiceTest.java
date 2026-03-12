package site.ticketlion.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.ticketlion.domain.Member;
import site.ticketlion.domain.MemberRole;
import site.ticketlion.repository.MemberRepository;
import site.ticketlion.web.dto.response.MemberResponse;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    @Test
    @DisplayName("회원 조회 성공")
    void getMember_success() {
        // given
        UUID memberId = UUID.randomUUID();
        Member member = new Member("Test User", "test@test.com", "password", "01012345678", MemberRole.USER);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        // when
        MemberResponse response = memberService.getMember(memberId);

        // then
        assertNotNull(response);
        assertEquals(member.getName(), response.name());
        assertEquals(member.getEmail(), response.email());
    }

    @Test
    @DisplayName("회원 조회 실패 - 존재하지 않는 회원")
    void getMember_fail_not_found() {
        // given
        UUID memberId = UUID.randomUUID();
        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(IllegalArgumentException.class, () -> memberService.getMember(memberId));
    }
}

package site.ticketlion.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import site.ticketlion.domain.Member;
import site.ticketlion.domain.MemberRole;
import site.ticketlion.repository.MemberRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberUserDetailsServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberUserDetailsService memberUserDetailsService;

    @Test
    @DisplayName("사용자 정보 로드 성공")
    void loadUserByUsername_success() {
        // given
        String email = "test@test.com";
        Member member = new Member("test", email, "password", "01012345678", MemberRole.USER);
        when(memberRepository.findByEmail(email)).thenReturn(Optional.of(member));

        // when
        UserDetails userDetails = memberUserDetailsService.loadUserByUsername(email);

        // then
        assertNotNull(userDetails);
        assertEquals(email, userDetails.getUsername());
    }

    @Test
    @DisplayName("사용자 정보 로드 실패 - 존재하지 않는 회원")
    void loadUserByUsername_fail_not_found() {
        // given
        String email = "test@test.com";
        when(memberRepository.findByEmail(email)).thenReturn(Optional.empty());

        // when & then
        assertThrows(UsernameNotFoundException.class, () -> memberUserDetailsService.loadUserByUsername(email));
    }
}

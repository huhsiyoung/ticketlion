package site.ticketlion.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import site.ticketlion.domain.Member;
import site.ticketlion.domain.MemberRole;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("이메일로 회원 찾기")
    void findByEmail() {
        // given
        String email = "test@test.com";
        Member member = new Member("test", email, "password", "01012345678", MemberRole.USER);
        memberRepository.save(member);

        // when
        Optional<Member> foundMember = memberRepository.findByEmail(email);

        // then
        assertTrue(foundMember.isPresent());
        assertEquals(email, foundMember.get().getEmail());
    }

    @Test
    @DisplayName("이메일 존재 여부 확인")
    void existsByEmail() {
        // given
        String email = "test@test.com";
        Member member = new Member("test", email, "password", "01012345678", MemberRole.USER);
        memberRepository.save(member);

        // when
        boolean exists = memberRepository.existsByEmail(email);

        // then
        assertTrue(exists);
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 회원 찾기")
    void findByEmail_not_found() {
        // when
        Optional<Member> foundMember = memberRepository.findByEmail("nonexistent@test.com");

        // then
        assertFalse(foundMember.isPresent());
    }

    @Test
    @DisplayName("존재하지 않는 이메일 존재 여부 확인")
    void existsByEmail_not_found() {
        // when
        boolean exists = memberRepository.existsByEmail("nonexistent@test.com");

        // then
        assertFalse(exists);
    }
}

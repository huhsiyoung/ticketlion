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
    @DisplayName("아이디로 회원 찾기")
    void findByUsername() {
        // given
        String username = "testuser";
        Member member = new Member("test", username, "password", MemberRole.USER);
        memberRepository.save(member);

        // when
        Optional<Member> foundMember = memberRepository.findByUsername(username);

        // then
        assertTrue(foundMember.isPresent());
        assertEquals(username, foundMember.get().getUsername());
    }

    @Test
    @DisplayName("아이디 존재 여부 확인")
    void existsByUsername() {
        // given
        String username = "testuser";
        Member member = new Member("test", username, "password", MemberRole.USER);
        memberRepository.save(member);

        // when
        boolean exists = memberRepository.existsByUsername(username);

        // then
        assertTrue(exists);
    }

    @Test
    @DisplayName("존재하지 않는 아이디로 회원 찾기")
    void findByUsername_not_found() {
        // when
        Optional<Member> foundMember = memberRepository.findByUsername("nonexistent");

        // then
        assertFalse(foundMember.isPresent());
    }

    @Test
    @DisplayName("존재하지 않는 아이디 존재 여부 확인")
    void existsByUsername_not_found() {
        // when
        boolean exists = memberRepository.existsByUsername("nonexistent");

        // then
        assertFalse(exists);
    }
}
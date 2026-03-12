package site.ticketlion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.ticketlion.domain.Member;
import site.ticketlion.domain.MemberRole;
import site.ticketlion.repository.MemberRepository;
import site.ticketlion.web.dto.request.SignupRequest;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;

    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void signup(SignupRequest req) {
        if (memberRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if (req.getPassword() == null || req.getPassword().length() < 8) {
            throw new IllegalArgumentException("비밀번호는 8자 이상이어야 합니다.");
        }
        if (!req.getPassword().equals(req.getPasswordConfirm())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        Member member = new Member(
            req.getName(),
            req.getEmail(),
            passwordEncoder.encode(req.getPassword()),
            req.getPhone(),
            MemberRole.USER
        );

        memberRepository.save(member);
    }
}

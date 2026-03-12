package site.ticketlion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import site.ticketlion.domain.MemberUserDetails;
import site.ticketlion.repository.MemberRepository;

@Service
@RequiredArgsConstructor
public class MemberUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return memberRepository.findByEmail(email)
            .map(MemberUserDetails::new)
            .orElseThrow(() -> new UsernameNotFoundException("회원이 존재하지 않습니다."));
    }
}

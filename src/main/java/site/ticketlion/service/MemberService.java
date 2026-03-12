package site.ticketlion.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import site.ticketlion.repository.MemberRepository;
import site.ticketlion.web.dto.response.MemberResponse;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberResponse getMember(UUID memberId) {

        return memberRepository.findById(memberId)
            .map(MemberResponse::from)
            .orElseThrow(() -> new IllegalArgumentException(memberId.toString()));
    }
}

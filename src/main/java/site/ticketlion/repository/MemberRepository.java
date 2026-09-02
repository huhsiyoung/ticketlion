package site.ticketlion.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import site.ticketlion.domain.Member;

public interface MemberRepository extends JpaRepository<Member, UUID> {

    Optional<Member> findByUsername(String username);

    boolean existsByUsername(String username);
}

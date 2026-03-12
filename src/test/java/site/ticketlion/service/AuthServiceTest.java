package site.ticketlion.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import site.ticketlion.domain.Member;
import site.ticketlion.repository.MemberRepository;
import site.ticketlion.web.dto.request.SignupRequest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private SignupRequest createSignupRequest(String name, String email, String password, String passwordConfirm, String phone) {
        SignupRequest request = new SignupRequest();
        request.setName(name);
        request.setEmail(email);
        request.setPassword(password);
        request.setPasswordConfirm(passwordConfirm);
        request.setPhone(phone);
        return request;
    }

    @Test
    @DisplayName("회원가입 성공")
    void signup_success() {
        // given
        SignupRequest request = createSignupRequest("test", "test@test.com", "password123", "password123", "01012345678");
        when(memberRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");

        // when
        authService.signup(request);

        // then
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 중복된 이메일")
    void signup_fail_duplicate_email() {
        // given
        SignupRequest request = createSignupRequest("test", "test@test.com", "password123", "password123", "01012345678");
        when(memberRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> authService.signup(request));
    }

    @Test
    @DisplayName("회원가입 실패 - 비밀번호 불일치")
    void signup_fail_password_mismatch() {
        // given
        SignupRequest request = createSignupRequest("test", "test@test.com", "password123", "wrongpassword", "01012345678");
        when(memberRepository.existsByEmail(request.getEmail())).thenReturn(false);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> authService.signup(request));
    }
}

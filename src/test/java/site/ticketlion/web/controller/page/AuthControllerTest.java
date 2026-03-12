package site.ticketlion.web.controller.page;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import site.ticketlion.config.SecurityConfigDev;
import site.ticketlion.service.AuthService;
import site.ticketlion.web.dto.request.SignupRequest;

@WebMvcTest(AuthController.class)
@Import(SecurityConfigDev.class)
@ActiveProfiles("dev")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("로그인 페이지를 보여준다")
    void loginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("signupRequest"));
    }

    @Test
    @DisplayName("회원가입 성공")
    void signup_success() throws Exception {
        doNothing().when(authService).signup(any(SignupRequest.class));

        mockMvc.perform(post("/signup").with(csrf())
                        .param("username", "testuser")
                        .param("password", "password")
                        .param("email", "test@example.com")
                        .param("phone", "010-1234-5678"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("successMessage", "회원가입이 완료되었습니다. 로그인 해주세요."));

        verify(authService).signup(any(SignupRequest.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 중복된 이메일")
    void signup_fail_duplicateEmail() throws Exception {
        doThrow(new IllegalArgumentException("이미 가입된 이메일입니다."))
                .when(authService).signup(any(SignupRequest.class));

        mockMvc.perform(post("/signup").with(csrf())
                        .param("username", "testuser")
                        .param("password", "password")
                        .param("email", "test@example.com")
                        .param("phone", "010-1234-5678"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("errorMessage", "⚠️ 이미 가입된 이메일입니다."));

        verify(authService).signup(any(SignupRequest.class));
    }
}

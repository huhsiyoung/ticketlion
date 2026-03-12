package site.ticketlion.web.controller.page;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import site.ticketlion.service.AuthService;
import site.ticketlion.web.dto.request.SignupRequest;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/login")
    public String loginPage(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            Object msg = session.getAttribute("LOGIN_ERROR");
            Object email = session.getAttribute("LOGIN_EMAIL");

            if (msg != null) model.addAttribute("errorMessage", msg.toString());
            if (email != null) model.addAttribute("email", email.toString());

            session.removeAttribute("LOGIN_ERROR");
            session.removeAttribute("LOGIN_EMAIL");
        }

        model.addAttribute("signupRequest", new SignupRequest());

        return "login";
    }

    @PostMapping("/signup")
    public String signup(
        @ModelAttribute SignupRequest signupRequest,
        RedirectAttributes attributes
    ) {
        try {
            authService.signup(signupRequest);
            attributes
                .addFlashAttribute("successMessage", "회원가입이 완료되었습니다. 로그인 해주세요.");

            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            attributes
                .addFlashAttribute("errorMessage", "⚠️ " + e.getMessage());

            return "redirect:/login";
        }
    }
}

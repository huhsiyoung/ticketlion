package site.ticketlion.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@Profile("dev")
@RequiredArgsConstructor
public class SecurityConfigDev {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(
                    "/h2-console/**",
                    "/events/*/holds",
                    "/reservations/payments",       // 결제 처리 API
                    "/reservations/*/assign"        // 좌석 배정 확정 API
                )
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin()) // H2 콘솔 iframe 허용
                .cacheControl(cache -> {})
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll() // ✅ H2 콘솔 열기
                .requestMatchers("/login", "/signup", "/css/**", "/js/**", "/images/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .defaultSuccessUrl("/events", true)
                .failureHandler((request, response, exception) -> {
                    HttpSession session = request.getSession();
                    session.setAttribute("LOGIN_ERROR", "⚠️ 이메일 또는 비밀번호가 올바르지 않습니다.");
                    session.setAttribute("LOGIN_EMAIL", request.getParameter("email"));
                    response.sendRedirect("/login"); // ✅ URL은 /login
                })
            )
            .rememberMe(rememberMe -> rememberMe
                .rememberMeParameter("remember-me")   // 체크박스 name과 일치
                .tokenValiditySeconds(60 * 60 * 24 * 14) // 14일 유지
                .key("ticketlion-remember-me-secret-key")
                .useSecureCookie(false) // dev 환경 HTTP이면 false, HTTPS면 true
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public OncePerRequestFilter noStoreFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                throws ServletException, IOException {

                chain.doFilter(req, res);

                String path = req.getRequestURI();
                // 보호 페이지들만 적용
                if (path.startsWith("/events") || path.startsWith("/reservation-details")) {
                    res.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                    res.setHeader("Pragma", "no-cache");
                    res.setDateHeader("Expires", 0);
                }
            }
        };
    }
}
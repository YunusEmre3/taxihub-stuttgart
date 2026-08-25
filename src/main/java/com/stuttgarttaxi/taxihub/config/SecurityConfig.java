package com.stuttgarttaxi.taxihub.config;

import com.stuttgarttaxi.taxihub.entity.Role;
import com.stuttgarttaxi.taxihub.service.EmployeeUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final EmployeeUserDetailsService employeeUserDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(employeeUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationSuccessHandler roleBasedSuccessHandler() {
        return (request, response, authentication) -> {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_" + Role.ADMIN.name()));
            String target = isAdmin ? "/admin/dashboard" : "/dashboard";
            response.sendRedirect(request.getContextPath() + target);
        };
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {
            if (exception instanceof DisabledException) {
                String email = URLEncoder.encode(
                        request.getParameter("email") == null ? "" : request.getParameter("email"),
                        StandardCharsets.UTF_8);
                response.sendRedirect(request.getContextPath() + "/login?error=unverified&email=" + email);
            } else {
                response.sendRedirect(request.getContextPath() + "/login?error=true");
            }
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // The public booking site has no server-rendered page/session to carry a
                // CSRF token (unlike the admin panel's <meta name="_csrf"> forms), so its
                // stateless JSON API is exempted here. CSRF stays enforced everywhere else.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/public/**", "/api/bookings/calculate-route"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/register", "/forgot-password", "/reset-password",
                                "/verify-email", "/verify-email/resend",
                                "/css/**", "/js/**", "/images/**").permitAll()
                        // Public customer booking site (frontend/) and its read-only API - no auth.
                        .requestMatchers("/", "/index.html", "/assets/**", "/api/public/**").permitAll()
                        // Shared by the public site and the internal booking-create page - read-only
                        // geocode+price lookup, no sensitive data, so it can't live under the
                        // ADMIN-only /api/bookings/** matcher below.
                        .requestMatchers(HttpMethod.POST, "/api/bookings/calculate-route").permitAll()
                        .requestMatchers("/admin/**", "/bookings/**", "/api/bookings/**", "/drivers/**",
                                "/customers/**", "/vehicles/**", "/reports/**", "/api/reports/**", "/settings/**")
                            .hasAuthority("ROLE_" + Role.ADMIN.name())
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(roleBasedSuccessHandler())
                        .failureHandler(authenticationFailureHandler())
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                );

        return http.build();
    }
}

package com.example.FinanceProject;

import com.example.FinanceProject.entity.User;
import com.example.FinanceProject.repository.UserRepo;
import com.example.FinanceProject.service.LoginAttemptService;
import com.example.FinanceProject.service.PasswordExpirationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.*;
import com.example.FinanceProject.service.CustomUserDetailsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final UserRepo userRepo;
    private final PasswordExpirationService passwordExpirationService;


    public SecurityConfig(CustomUserDetailsService userDetailsService, UserRepo userRepo, PasswordExpirationService passwordExpirationService) {
        this.userDetailsService = userDetailsService;
        this.userRepo = userRepo;
        this.passwordExpirationService = passwordExpirationService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disable CSRF for simplicity (enable in production)
            .headers(headers -> headers
                .frameOptions(options -> options.sameOrigin()) // Allows framing from same origin
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**", "/auth/registration", "/auth/register", "/forgot-password", "/reset-password", "/password-reset-success", "/api/password/forgot", "/api/password/reset/validate", "/api/password/reset", "/password-expired").permitAll() // Allow public access
                .requestMatchers("/admin/**").hasRole("ADMIN") // Restrict admin routes
                .requestMatchers("/accounts/add").hasRole("ADMIN") // Only admin can access account-add
                .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN") // Restrict user routes
                .anyRequest().authenticated()
            )
            .formLogin(login -> login
                .loginPage("/login").permitAll()
                .successHandler(customAuthenticationSuccessHandler())
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login").permitAll()
            ).addFilterBefore(passwordExpirationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordExpirationFilter passwordExpirationFilter() {
        // Use UserRepo instead of UserService
        return new PasswordExpirationFilter(userRepo, passwordExpirationService);
    }

    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, 
                                                HttpServletResponse response, 
                                                Authentication authentication) throws IOException, ServletException {
                // Check if the logged in user has ROLE_ADMIN
                boolean isAdmin = authentication.getAuthorities().stream()
                                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
                boolean isManager = authentication.getAuthorities().stream()
                        .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_MANAGER"));
                boolean isUser = authentication.getAuthorities().stream()
                        .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_USER"));
                if (isAdmin) {
                    response.sendRedirect("/admin");
                } else if (isManager) {
                    response.sendRedirect("/manager");
                } else if (isUser) {
                    response.sendRedirect("/accountant");
                } else {
                    // For non-admin users, redirect to a default user page or home page
                    response.sendRedirect("/user");
                }
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    @Bean
    public AuthenticationSuccessHandler loginSuccessHandler() {
        return new SimpleUrlAuthenticationSuccessHandler() {
            @Autowired
            private LoginAttemptService loginAttemptService;

            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                                Authentication authentication) throws IOException, ServletException {

                // Reset failed attempts for successful login
                loginAttemptService.resetFailedAttempts(authentication.getName());

                super.onAuthenticationSuccess(request, response, authentication);
            }
        };
    }

    @Bean
    public AuthenticationFailureHandler loginFailureHandler() {
        return new SimpleUrlAuthenticationFailureHandler() {
            @Autowired
            private LoginAttemptService loginAttemptService;

            @Autowired
            private UserRepo userRepo;

            @Override
            public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                                AuthenticationException exception) throws IOException, ServletException {

                String username = request.getParameter("username");
                User user = userRepo.findByUsername(username).orElse(null);

                if (user != null) {
                    loginAttemptService.increaseFailedAttempts(user);

                    if (user.getFailedAttempt() >= LoginAttemptService.MAX_FAILED_ATTEMPTS) {
                        loginAttemptService.lockUser(user);
                        exception = new LockedException("Your account has been locked due to 3 failed attempts.");
                    }
                }

                super.onAuthenticationFailure(request, response, exception);
            }
        };
    }
}

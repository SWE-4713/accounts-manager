package com.example.FinanceProject;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.FinanceProject.entity.User;
import com.example.FinanceProject.repository.UserRepo;
import com.example.FinanceProject.service.CustomUserDetailsService;
import com.example.FinanceProject.service.LoginAttemptService;
import com.example.FinanceProject.service.PasswordExpirationService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers
                .frameOptions(options -> options.sameOrigin())
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**", "/auth/registration", "/auth/register", "/forgot-password", "/reset-password", "/password-reset-success", "/api/password/**", "/password-expired", "/update-expired-password", "/login").permitAll() // Ensure login and password reset pages are permitted
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/accounts/add", "/accounts/edit", "/accounts/deactivate").hasRole("ADMIN") // Admin only account modification
                // Permit dashboard for all authenticated users
                .requestMatchers("/dashboard").authenticated()
                // Keep other specific role restrictions as needed
                .requestMatchers("/reports/**").hasAnyRole("ADMIN", "MANAGER") // Example: Reports for Admin/Manager
                .requestMatchers("/manager/**").hasRole("MANAGER")
                .requestMatchers("/accountant/**").hasRole("USER") // Assuming ROLE_USER is accountant
                .anyRequest().authenticated() // All other requests need authentication
            )
            .formLogin(login -> login
                .loginPage("/login").permitAll()
                .successHandler(customAuthenticationSuccessHandler()) // Use the updated success handler
                // Add failure handler if not already present
                // .failureHandler(loginFailureHandler())
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout").permitAll() // Redirect to login page with logout message
            )
            .addFilterBefore(passwordExpirationFilter(), UsernamePasswordAuthenticationFilter.class); // Keep password expiration filter

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

                // Check for password expiration FIRST (optional but good practice)
                // User user = userRepo.findByUsername(authentication.getName()).orElse(null);
                // if (user != null && passwordExpirationService.isPasswordExpired(user)) {
                //     response.sendRedirect("/password-expired"); // Redirect to change password page
                //     return;
                // }

                // Redirect based on role (or always to dashboard)
                boolean isAdmin = authentication.getAuthorities().stream()
                                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
                boolean isManager = authentication.getAuthorities().stream()
                        .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_MANAGER"));
                boolean isUser = authentication.getAuthorities().stream()
                        .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_USER"));

                // --- MODIFICATION START ---
                // Redirect all authenticated users to the dashboard
                response.sendRedirect("/dashboard");

                // --- Original Role-Based Redirect (Keep for reference or revert if needed) ---
                /*
                if (isAdmin) {
                    response.sendRedirect("/admin/user-management"); // Or /admin if preferred
                } else if (isManager) {
                    // Managers might also go to the dashboard, or a specific manager view
                    response.sendRedirect("/dashboard"); // Or /manager if needed
                } else { // Default for ROLE_USER or others
                    response.sendRedirect("/dashboard"); // Redirect standard users to dashboard
                }
                */
                // --- MODIFICATION END ---
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

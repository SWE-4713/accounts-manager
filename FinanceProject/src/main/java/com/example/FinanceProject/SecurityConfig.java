package com.example.FinanceProject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
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

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disable CSRF for simplicity (enable in production)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**", "/auth/registration", "/auth/register").permitAll() // Allow public access
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
            );

        return http.build();
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
                if (isAdmin) {
                    response.sendRedirect("/admin");
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
}

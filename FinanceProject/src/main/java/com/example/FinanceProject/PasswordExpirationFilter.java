package com.example.FinanceProject;

import com.example.FinanceProject.User;
import com.example.FinanceProject.repository.UserRepo;
import com.example.FinanceProject.service.PasswordExpirationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class PasswordExpirationFilter extends OncePerRequestFilter {

    private final UserRepo userRepo;
    private final PasswordExpirationService passwordExpirationService;

    private List<String> excludedUrls = Arrays.asList("/password-expired",
            "/update-expired-password",
            "/css/", "/js/", "/images/",
            "/login", "/logout", "/register", "/reset-password");

    // Constructor-based dependency injection
    public PasswordExpirationFilter(UserRepo userRepo, PasswordExpirationService passwordExpirationService) {
        this.userRepo = userRepo;
        this.passwordExpirationService = passwordExpirationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip check for excluded URLs
        if (excludedUrls.stream().anyMatch(path::startsWith)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser")) {
            String username = authentication.getName();
            // Use UserRepo directly to find the user by username
            Optional<User> userOptional = userRepo.findByUsername(username);

            if (userOptional.isPresent() && passwordExpirationService.isPasswordExpired(userOptional.get())) {
                response.sendRedirect(request.getContextPath() + "/password-expired");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
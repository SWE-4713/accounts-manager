package com.example.FinanceProject; // Adjust the package as needed

import com.example.FinanceProject.service.LoginAttemptService;
import com.example.FinanceProject.service.LoginLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;

import com.example.FinanceProject.User;
import com.example.FinanceProject.repository.UserRepo;

@Component
public class AuthenticationFailureEventListener {
    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private LoginLogService loginLogService;

    @Autowired
    private UserRepo userRepo;

    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        String username = event.getAuthentication().getName();
        User user = userRepo.findByUsername(username).orElse(null);

        if (user != null) {
            loginAttemptService.increaseFailedAttempts(user);
        }
        // Log the failed login attempt regardless of whether the user exists
        loginLogService.recordLogin(user, username, false);
    }
}
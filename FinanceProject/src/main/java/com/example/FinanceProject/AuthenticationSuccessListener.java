package com.example.FinanceProject;

import com.example.FinanceProject.entity.User;
import com.example.FinanceProject.repository.UserRepo;
import com.example.FinanceProject.service.PasswordExpirationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuthenticationSuccessListener implements ApplicationListener<AuthenticationSuccessEvent> {

    @Autowired
    private PasswordExpirationService passwordExpirationService;

    @Autowired
    private UserRepo userRepo; // Use UserRepo directly instead of UserService

    @Override
    public void onApplicationEvent(AuthenticationSuccessEvent event) {
        if (event.getAuthentication().getPrincipal() instanceof UserDetails) {
            String username = ((UserDetails) event.getAuthentication().getPrincipal()).getUsername();
            Optional<User> userOpt = userRepo.findByUsername(username); // Use UserRepo's method

            // Check if password is expired
            if (userOpt.isPresent() && passwordExpirationService.isPasswordExpired(userOpt.get())) {
                // Redirect to password change page or set a flag in the session
                // This would need to be handled in a controller
            }
        }
    }
}
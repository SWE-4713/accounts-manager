package com.example.FinanceProject.controller;

import com.example.FinanceProject.entity.User;
import com.example.FinanceProject.service.PasswordExpirationService;
import com.example.FinanceProject.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class PasswordExpirationController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordExpirationService passwordExpirationService;

    @GetMapping("/password-expired")
    public String passwordExpired(Model model, Authentication authentication) {
        // Get the authenticated user
        User user = userService.findUserByUsername(authentication.getName());

        // Add the user ID to the model
        model.addAttribute("userId", user.getId());

        return "password-expired";
    }

    @PostMapping("/update-expired-password")
    public String updateExpiredPassword(@RequestParam String newPassword,
                                        Authentication authentication) {
        User user = userService.findUserByUsername(authentication.getName());
        user.setPassword(userService.encodePassword(newPassword));
        passwordExpirationService.updatePasswordDate(user);
        userService.saveUser(user);

        return "redirect:/logout";
    }
}
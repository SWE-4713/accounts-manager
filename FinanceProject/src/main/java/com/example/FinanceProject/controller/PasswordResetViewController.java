package com.example.FinanceProject.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PasswordResetViewController {

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password"; // Refers to forgot-password.html in your templates directory
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam(required = false) String token) {
        // The token will be handled by front-end JavaScript
        return "reset-password"; // Refers to reset-password.html in your templates directory
    }

    @GetMapping("/password-reset-success")
    public String passwordResetSuccessPage() {
        return "password-reset-success"; // Refers to password-reset-success.html in your templates directory
    }
}
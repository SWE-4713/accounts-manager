package com.example.FinanceProject.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PublicController {

    // Existing mapping for public page
    @GetMapping("/public")
    public String publicPage() {
        return "user-page";  // Returns the user-page view
    }

    // New mapping for user dashboard (for non-admins)
    @GetMapping("/user")
    public String userDashboard() {
        return "user-page";  // Returns the user-page view
    }

    // Registration mapping remains unchanged
    @GetMapping("/auth/registration")
    public String registrationPage() {
        return "registration_page";
    }
}


package com.example.FinanceProject.controller;

import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    // This will serve the Admin landing page
    @GetMapping("/admin")
    @Secured("ROLE_ADMIN") // Only accessible to users with 'ROLE_ADMIN'
    public String adminLandingPage() {
        return "admin-landing"; // Return the name of the HTML page (Thymeleaf template)
    }
}


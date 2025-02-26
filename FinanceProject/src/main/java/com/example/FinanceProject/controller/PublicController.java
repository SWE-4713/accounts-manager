package com.example.FinanceProject.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PublicController {

    // This will serve the public page
    @GetMapping("/public")
    public String publicPage() {
        return "user-page";  // Return the name of the HTML page (Thymeleaf template)
    }

    @GetMapping("/auth/registration")
    public String registrationPage() {
        return "registration";  // Return the name of the HTML page (Thymeleaf template)
    }
}


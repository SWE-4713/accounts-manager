package com.example.FinanceProject.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/public/registrationConfirmation")
    public String registrationConfirmation() {
        return "registrationConfirmation";
    }
}

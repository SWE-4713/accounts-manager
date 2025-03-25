package com.example.FinanceProject.controller;

import com.example.FinanceProject.service.EmailService;
import com.example.FinanceProject.service.PasswordExpirationReportService;
import com.example.FinanceProject.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Secured("ROLE_USER")
@RequestMapping("/accountant")
public class AccountantController {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordExpirationReportService passwordExpirationReportService;

    // Serve the admin landing page with both active and pending users
    @GetMapping
    public String managerLandingPage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("username", authentication.getName());
        model.addAttribute("pendingUsers", userService.getAllPendingUsers());
        return "redirect:/accounts";
    }
}

package com.example.FinanceProject.controller;

import com.example.FinanceProject.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/auth")
public class UserController {

    @Autowired
    private UserService userService;

    // This method processes the registration form submission
    @PostMapping("/register")
    public String registerUser(@RequestParam String username,
                               @RequestParam String password,
                               @RequestParam String role,
                               @RequestParam String firstName,
                               @RequestParam String lastName,
                               @RequestParam String address,
                               @RequestParam String dob,
                               @RequestParam String email,
                               Model model) {
        try {
            // Create and save the new user
            userService.registerUser(username, password, role, firstName, lastName, address, dob, email);
            // Redirect to a confirmation page after successful registration
            return "registrationConfirmation";
        } catch (Exception e) {
            // If an error occurs (e.g., duplicate username), send the error back to the registration page.
            model.addAttribute("error", "Error registering user: " + e.getMessage());
            return "registration";
        }
    }
}

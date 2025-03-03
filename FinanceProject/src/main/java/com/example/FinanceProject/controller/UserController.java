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

    // This method processes self-registration
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
            // Instead of immediately adding to live users,
            // add to pending users.
            userService.registerPendingUser(username, password, role, firstName, lastName, address, dob, email);
            return "registrationConfirmation";
        } catch (Exception e) {
            model.addAttribute("error", "Error registering user: " + e.getMessage());
            return "registration";
        }
    }
}

package com.example.FinanceProject.controller;

import com.example.FinanceProject.service.EmailService;
import com.example.FinanceProject.service.UserService;
import com.example.FinanceProject.util.PasswordValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/auth")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService; // Add this

    // This method processes self-registration
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(
            @RequestParam String role,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String address,
            @RequestParam String dob,
            @RequestParam String email,
            Model model) {
//        if (!PasswordValidator.isValid(password)) {
//            Map<String, String> errors = new HashMap<>();
//            errors.put(
//                    "password",
//                    "Password must contain at least one letter, one number, one special character, and be at least 8 characters long.");
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
//        }

        try {
            // This method now sends emails to all admins automatically
            userService.registerPendingUser(
                    role,
                    firstName,
                    lastName,
                    address,
                    dob,
                    email);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("User registration pending approval");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error registering user: " + e.getMessage());
        }
    }
}

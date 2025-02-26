package com.example.FinanceProject.controller;

import com.example.FinanceProject.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestParam String username,
                                               @RequestParam String password,
                                               @RequestParam String role,
                                               @RequestParam String firstName,
                                               @RequestParam String lastName,
                                               @RequestParam String address,
                                               @RequestParam String dob,
                                               @RequestParam String email) {
        try {
            userService.registerUser(username, password, role, firstName, lastName, address, dob, email);
            return ResponseEntity.ok("User registered successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error registering user: " + e.getMessage());
        }
    }
}

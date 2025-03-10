package com.example.FinanceProject.controller;

import com.example.FinanceProject.dto.RegistrationRequest;
import com.example.FinanceProject.util.PasswordValidator;
import com.example.FinanceProject.service.UserService; // Assuming you have a UserService
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/auth")
public class RegistrationController {

    private final UserService userService;

    public RegistrationController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("registrationRequest", new RegistrationRequest());
        return "registration_page"; // Replace with your registration form view name
    }

    @PostMapping("/register_tmp")
    public ResponseEntity<?> registerUser(
            @Valid @ModelAttribute("registrationRequest") RegistrationRequest request,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
            return ResponseEntity.badRequest().body(errors);
        }

        if (!PasswordValidator.isValid(request.getPassword())) {
            Map<String, String> errors = new HashMap<>();
            errors.put("password", "Password must contain at least one letter, one number, and one special character.");
            return ResponseEntity.badRequest().body(errors);
        }

        // If validation passes, create the user
        try {
            userService.registerNewUser(request); // Implement this method in UserService
            return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to register user: " + e.getMessage());
        }
    }
}

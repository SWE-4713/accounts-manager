package com.example.FinanceProject.controller;

import com.example.FinanceProject.User;
import com.example.FinanceProject.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/password")
public class PasswordResetController {

    @Autowired
    private UserService userService;

    @PostMapping("/forgot")
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {
        boolean result = userService.initiatePasswordReset(email);
        
        // Always return success to prevent email enumeration attacks
        Map<String, String> response = new HashMap<>();
        response.put("message", "If the email exists in our system, a reset link has been sent.");
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/reset/validate")
    public ResponseEntity<?> validateResetToken(@RequestParam String token) {
        User user = userService.validatePasswordResetToken(token);
        
        Map<String, Object> response = new HashMap<>();
        if (user != null) {
            response.put("valid", true);
            return ResponseEntity.ok(response);
        } else {
            response.put("valid", false);
            response.put("message", "Invalid or expired token");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<?> resetPassword(@RequestParam String token, @RequestParam String password) {
        Map<String, Object> response = new HashMap<>();

        try {
            boolean result = userService.resetPassword(token, password);

            if (result) {
                response.put("success", true);
                response.put("message", "Password has been reset successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Invalid or expired token");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (UserService.PasswordReusedException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
package com.example.FinanceProject.controller;

import com.example.FinanceProject.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@Secured("ROLE_ADMIN")
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    // Serve the admin landing page with a list of existing users
    @GetMapping
    public String adminLandingPage(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "admin-landing";
    }

    // Handle the form submission to create a new user
    @PostMapping("/users/create")
    public String createUser(@RequestParam String username,
                             @RequestParam String password,
                             @RequestParam String role,
                             @RequestParam String firstName,
                             @RequestParam String lastName,
                             @RequestParam String address,
                             @RequestParam String dob,
                             @RequestParam String email,
                             Model model) {
        try {
            userService.registerUser(username, password, role, firstName, lastName, address, dob, email);
            model.addAttribute("message", "User created successfully.");
        } catch (Exception e) {
            model.addAttribute("error", "Error creating user: " + e.getMessage());
        }
        // Refresh the list of users after creation
        model.addAttribute("users", userService.getAllUsers());
        return "admin-landing";
    }

    // Handle deletion of a user based on the user's id
    @PostMapping("/users/delete")
    public String deleteUser(@RequestParam Long id, Model model) {
        try {
            userService.deleteUser(id);
            model.addAttribute("message", "User deleted successfully.");
        } catch (Exception e) {
            model.addAttribute("error", "Error deleting user: " + e.getMessage());
        }
        // Refresh the list of users after deletion
        model.addAttribute("users", userService.getAllUsers());
        return "admin-landing";
    }
}

package com.example.FinanceProject.controller;

import com.example.FinanceProject.entity.User;
import com.example.FinanceProject.service.EmailService;
import com.example.FinanceProject.service.PasswordExpirationReportService;
import com.example.FinanceProject.service.UserService;
import com.example.FinanceProject.service.EventLogService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.*;



@Controller
@Secured("ROLE_ADMIN")
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordExpirationReportService passwordExpirationReportService;

    @Autowired
    private EventLogService eventLogService;


    // Serve the admin landing page with both active and pending users
    @GetMapping
    public String adminLandingPage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("username", authentication.getName());
        model.addAttribute("pendingUsers", userService.getAllPendingUsers());
        return "redirect:/accounts";
    }

    @GetMapping("/user-management")
    public String userManagement(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("username", authentication.getName());
        model.addAttribute("pendingUsers", userService.getAllPendingUsers());
        return "user-management";
    }

    @GetMapping("/password/expired-report")
    public ResponseEntity<Map<String, Object>> generateExpiredPasswordsReport() {
        List<User> expiredUsers = passwordExpirationReportService.findUsersWithExpiredPasswords();

        List<Map<String, Object>> usersList = expiredUsers.stream()
                .map(user -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("username", user.getUsername());
                    userMap.put("email", user.getEmail());
                    userMap.put("lastPasswordChange", user.getPasswordLastChanged());

                    // Calculate days expired
                    long diffInMillies = new Date().getTime() - user.getPasswordLastChanged().getTime();
                    long diffInDays = diffInMillies / (1000 * 60 * 60 * 24);
                    userMap.put("daysExpired", diffInDays);

                    return userMap;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("count", expiredUsers.size());
        response.put("users", usersList);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/password/expired-report/email")
    public ResponseEntity<String> emailExpiredPasswordsReport() {
        System.out.println("Sending expired passwords report to admin email");
        passwordExpirationReportService.sendExpiredPasswordsReport();
        return ResponseEntity.ok("Expired passwords report sent to admin email");
    }

    @PostMapping("/send-email")
    public String sendEmail(
            @RequestParam("recipientUsername") String username,
            @RequestParam("subject") String subject,
            @RequestParam("message") String message,
            RedirectAttributes redirectAttributes) {

        try {
            // Get user email from username
            User user = userService.findUserByUsername(username);
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "User not found");
                return "redirect:/admin/user-management";
            }

            // Send email
            emailService.sendSimpleEmail(user.getEmail(), subject, message);

            // Add success message
            redirectAttributes.addFlashAttribute("message", "Email sent successfully to " + username);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to send email: " + e.getMessage());
        }

        return "redirect:/admin/user-management";
    }

    // Handle the creation of a new user via admin form
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
            userService.registerSetUser(username, password, role, firstName, lastName, address, dob, email);
            model.addAttribute("message", "User created successfully.");
        } catch (Exception e) {
            model.addAttribute("error", "Error creating user: " + e.getMessage());
        }
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("pendingUsers", userService.getAllPendingUsers());
        return "user-management";
    }

    // Accept a pending registration
    @PostMapping("/pending/accept")
    public String acceptPending(@RequestParam Long id, Model model) {
        try {
            userService.acceptPendingUser(id);
            model.addAttribute("message", "User accepted successfully.");
        } catch (Exception e) {
            model.addAttribute("error", "Error accepting user: " + e.getMessage());
        }
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("pendingUsers", userService.getAllPendingUsers());
        return "user-management";
    }

    // Deny a pending registration
    @PostMapping("/pending/deny")
    public String denyPending(@RequestParam Long id, Model model) {
        try {
            userService.denyPendingUser(id);
            model.addAttribute("message", "User denied successfully.");
        } catch (Exception e) {
            model.addAttribute("error", "Error denying user: " + e.getMessage());
        }
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("pendingUsers", userService.getAllPendingUsers());
        return "user-management";
    }
    
    @PostMapping("/users/suspend")
    public String suspendUser(@RequestParam Long id, Model model) {
        try {
            userService.suspendUser(id);
            model.addAttribute("message", "User suspended successfully.");
        } catch (Exception e) {
            model.addAttribute("error", "Error suspending user: " + e.getMessage());
        }
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("pendingUsers", userService.getAllPendingUsers());
        return "user-management";
    }
    
    @PostMapping("/users/unsuspend")
    public String unsuspendUser(@RequestParam Long id, Model model) {
        try {
            userService.unsuspendUser(id);
            model.addAttribute("message", "User unsuspended successfully.");
        } catch (Exception e) {
            model.addAttribute("error", "Error unsuspending user: " + e.getMessage());
        }
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("pendingUsers", userService.getAllPendingUsers());
        return "user-management";
    }

    @GetMapping("/users/edit")
    public String showEditUserForm(@RequestParam("id") Long id, Model model) {
        User user = userService.getUserById(id);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("username", authentication.getName());
        model.addAttribute("user", user);
        return "edit-user";
    }

    @PostMapping("/users/update")
    public String updateUser(@RequestParam("id") Long id, 
                             @RequestParam("username") String username,
                             @RequestParam("role") String role,
                             @RequestParam("firstName") String firstName,
                             @RequestParam("lastName") String lastName,
                             @RequestParam("address") String address,
                             @RequestParam("dob") String dob,
                             @RequestParam("email") String email,
                             RedirectAttributes redirectAttributes) {
        
        userService.updateUser(id, username, role, firstName, lastName, address, dob, email);
        redirectAttributes.addFlashAttribute("message", "User updated successfully");
        return "redirect:/admin/user-management";
    }

    // Endpoint to view event logs
    @GetMapping("/event-logs")
    public String eventLogs(Model model) {
        model.addAttribute("eventLogs", eventLogService.getAllEventLogs());
        model.addAttribute("username", SecurityContextHolder.getContext().getAuthentication().getName());
        return "event-logs";
    }
}

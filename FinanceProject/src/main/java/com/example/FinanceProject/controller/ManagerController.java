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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.FinanceProject.entity.JournalEntry;
import com.example.FinanceProject.service.JournalEntryService;

import java.util.List;

@Controller
@Secured("ROLE_MANAGER")
@RequestMapping("/manager")
public class ManagerController {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordExpirationReportService passwordExpirationReportService;

    @Autowired
    private JournalEntryService journalEntryService;

    // Serve the admin landing page with both active and pending users
    @GetMapping
    public String managerLandingPage(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("username", authentication.getName());
        model.addAttribute("pendingUsers", userService.getAllPendingUsers());
        return "redirect:/accounts";
    }

    @GetMapping("/journal/pending")
    public String viewPendingJournalEntries(Model model) {
        List<JournalEntry> pendingEntries = journalEntryService.findByStatus("PENDING");
        model.addAttribute("journalEntries", pendingEntries);
        model.addAttribute("username", SecurityContextHolder.getContext().getAuthentication().getName());
        return "redirect:/journal"; // Create a corresponding Thymeleaf template
    }

    // Approve a journal entry
    @PostMapping("/journal/approve")
    public String approveJournalEntry(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        try {
            journalEntryService.approveEntry(id);
            redirectAttributes.addFlashAttribute("message", "Journal entry approved.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/journal";
    }

    // Reject a journal entry (with comment)
    @PostMapping("/journal/reject")
    public String rejectJournalEntry(@RequestParam Long id,
                                     @RequestParam String comment,
                                     RedirectAttributes redirectAttributes) {
        try {
            journalEntryService.rejectEntry(id, comment);
            redirectAttributes.addFlashAttribute("message", "Journal entry rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/journal";
    }
}

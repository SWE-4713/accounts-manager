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
import java.util.*;

import com.example.FinanceProject.entity.JournalEntry;
import com.example.FinanceProject.service.JournalEntryService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    // Display the journal entry creation page
    @GetMapping("/journal/create")
    public String createJournalEntryForm(Model model) {
        model.addAttribute("journalEntry", new JournalEntry());
        return "journal-create"; // Create the Thymeleaf template
    }

    // Handle journal entry submission with file attachment (source documents)
    // AccountantController.java
    @PostMapping("/journal/create")
    public String submitJournalEntry(@ModelAttribute JournalEntry journalEntry,
                                    @RequestParam("attachment") MultipartFile attachment,
                                    Model model, RedirectAttributes redirectAttributes) {
        try {
            if (attachment != null && !attachment.isEmpty()) {
                String filePath = journalEntryService.storeAttachment(attachment);
                journalEntry.setAttachmentPath(filePath);
            }
            
            // Updated: now returns a single JournalEntry
            JournalEntry savedEntry = journalEntryService.submitJournalEntry(journalEntry);
            
            redirectAttributes.addFlashAttribute("message", 
                "Journal entry submitted successfully with ID: " + savedEntry.getId());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Submission failed: " + e.getMessage());
            return "redirect:/accountant/journal/create";
        }
        return "redirect:/accountant/journal/create";
    }
}

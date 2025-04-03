package com.example.FinanceProject.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import com.example.FinanceProject.entity.JournalEntry;
import com.example.FinanceProject.service.EmailService;
import com.example.FinanceProject.service.JournalEntryService;
import org.springframework.web.multipart.MultipartFile;


@Controller
@Secured({"ROLE_MANAGER", "ROLE_USER"})
@RequestMapping("/journal")
public class JournalController {

    @Autowired
    private JournalEntryService journalEntryService;

    @Autowired
    private EmailService emailService;

    // Show form for creating a new journal entry
    @GetMapping("/new")
    public String newJournalEntry(Model model) {
        // Load list of accounts (using AccountService, not shown here) to populate the form
        model.addAttribute("journalEntry", new JournalEntry());
        return "journal-entry-form"; // Create a Thymeleaf template for the journal entry form
    }

    // Submit (or save) a journal entry. For accountants: they can attach a file.
    @PostMapping("/submit")
    public String submitJournalEntry(@ModelAttribute JournalEntry journalEntry,
                                     @RequestParam(value = "attachment", required = false) MultipartFile attachment,
                                     Model model) {
        try {
            // Handle file attachment if provided (store the file and set attachmentPath)
            if (attachment != null && !attachment.isEmpty()) {
                // For example, save the file to disk and get the file path
                String filePath = journalEntryService.storeAttachment(attachment);
                journalEntry.setAttachmentPath(filePath);
            }

            // Validate journal entry: must have at least one debit and one credit, and debits equal credits
            JournalEntry submittedEntry = journalEntryService.submitJournalEntry(journalEntry);

            // Send notification email to manager upon submission (notification logic can be more advanced)
            emailService.sendSimpleEmail("manager@example.com", "New Journal Entry Submitted",
                    "A new journal entry has been submitted for your approval. Please review it in the system.");

            model.addAttribute("message", "Journal entry submitted successfully and sent for approval.");
            return "redirect:/journal/pending";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "journal-entry-form";
        }
    }

    // List journal entries by status (pending, approved, rejected), with optional filtering by date or search term.
    @GetMapping("/{status}")
    public String listJournalEntries(@PathVariable String status,
                                     @RequestParam(required = false) String dateFrom,
                                     @RequestParam(required = false) String dateTo,
                                     @RequestParam(required = false) String search,
                                     Model model) {
        List<JournalEntry> entries = journalEntryService.getJournalEntriesByStatus(status, dateFrom, dateTo, search);
        model.addAttribute("entries", entries);
        model.addAttribute("status", status);
        return "journal-list"; // Create a Thymeleaf template that lists journal entries
    }

    // Approve a journal entry (for manager)
    @PostMapping("/approve")
    public String approveJournalEntry(@RequestParam Long id) {
        journalEntryService.approveEntry(id);
        return "redirect:/journal/pending";
    }

    // Reject a journal entry (for manager); the manager must supply a comment (reason)
    @PostMapping("/reject")
    public String rejectJournalEntry(@RequestParam Long id, @RequestParam String comment) {
        journalEntryService.rejectEntry(id, comment);
        return "redirect:/journal/pending";
    }

    // View journal entry details by post reference
    @GetMapping("/view/{postReference}")
    public String viewJournalEntryByPostReference(@PathVariable String postReference, Model model) {
        JournalEntry entry = journalEntryService.getJournalEntryByPostReference(postReference);
        model.addAttribute("journalEntry", entry);
        return "journal-entry-view"; // Create a view for showing the journal entry details
    }
}

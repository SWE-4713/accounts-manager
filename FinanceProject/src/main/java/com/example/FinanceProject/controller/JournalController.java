package com.example.FinanceProject.controller;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;

import com.example.FinanceProject.entity.Account;
import com.example.FinanceProject.entity.JournalEntry;
import com.example.FinanceProject.entity.JournalEntryLine;
import com.example.FinanceProject.entity.JournalStatus;
import com.example.FinanceProject.service.AccountService;
import com.example.FinanceProject.service.EmailService;
import com.example.FinanceProject.service.JournalEntryService;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;    


@Controller
@Secured({"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_USER"})
@RequestMapping("/journal")
public class JournalController {

    @Autowired
    private JournalEntryService journalEntryService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AccountService accountService;

    @GetMapping
    public String showAllJournalEntries(@RequestParam(required = false) String status,
                                        @RequestParam(required = false) String startDate,
                                        @RequestParam(required = false) String endDate,
                                        @RequestParam(required = false) String search,
                                        Model model) {
        // If you want to filter by status/date, do so in your service:
        List<JournalEntry> entries = journalEntryService.getAllEntriesFiltered(status, startDate, endDate, search);
        
        // Put them in the model
        model.addAttribute("entries", entries);
        return "journal"; // The single table from above
    }

    @GetMapping("/new")
    public String newJournalEntry(Model model) {
        if (!model.containsAttribute("journalEntry")) {
            model.addAttribute("journalEntry", new JournalEntry());
        }
        List<Account> accounts = accountService.getAllAccounts(Sort.by("accountNumber"));
        model.addAttribute("accounts", accounts);
        return "journal-entry-form";
    }

    // JournalController.java
    @PostMapping("/submit")
    public String submitJournalEntry(
            @ModelAttribute JournalEntry journalEntry,
            @RequestParam(value = "attachment", required = false) MultipartFile attachment,
            Model model, RedirectAttributes redirectAttributes) {
        try {
            // Handle file attachment if provided.
            if (attachment != null && !attachment.isEmpty()) {
                String filePath = journalEntryService.storeAttachment(attachment);
                journalEntry.setAttachmentPath(filePath);
            }

            // Process each line as an independent journal entry.
            List<JournalEntry> savedEntries = journalEntryService.submitJournalEntry(journalEntry);

            // Optionally notify the manager.
            emailService.sendSimpleEmail("manager@example.com", "New Journal Entry Submitted",
                    "New journal entry lines have been submitted for approval. Please review them in the system.");

            redirectAttributes.addFlashAttribute("message", 
                    savedEntries.size() + " journal entry line(s) submitted successfully.");
            return "redirect:/journal";  // Redirect to the journal page
        } catch (Exception e) {
            // Pass the error message to the view.
            redirectAttributes.addFlashAttribute("error", "Submission failed: " + e.getMessage());
            List<Account> accounts = accountService.getAllAccounts(Sort.by("accountNumber"));
            model.addAttribute("accounts", accounts);
            return "journal-entry-form";  // Re-display the entry form in case of error
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
        return "journal";
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

package com.example.FinanceProject.controller;

import java.time.LocalDate;
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
import com.example.FinanceProject.repository.JournalEntryRepo;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;    


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
                                        Model model,
                                        Authentication authentication) {
        // Retrieve entries using our filtering method.
        List<JournalEntry> entries = journalEntryService.getAllEntriesFiltered(status, startDate, endDate, search);
        model.addAttribute("entries", entries);
        // Pass the parameters back to the view to pre-populate the filter form
        model.addAttribute("status", status);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("search", search);
        model.addAttribute("username", authentication.getName());
        return "journal"; // This maps to journal.html
    }


    @GetMapping("/new")
    public String newJournalEntry(Model model, Authentication authentication) {
        if (!model.containsAttribute("journalEntry")) {
            JournalEntry newEntry = new JournalEntry();
            newEntry.setEntryDate(LocalDate.now());
            // Preload with two empty lines
            newEntry.getLines().add(new JournalEntryLine());
            newEntry.getLines().add(new JournalEntryLine());
            model.addAttribute("journalEntry", newEntry);
        }
        // Get the list of accounts to populate the select list in the form.
        List<Account> accounts = accountService.getAllAccounts(Sort.by("accountNumber"));
        model.addAttribute("accounts", accounts);
        // Pass logged-in username for display in the navbar
        model.addAttribute("username", authentication.getName());
        return "journal-entry-form";
    }

    @PostMapping("/submit")
    public String submitJournalEntry(@ModelAttribute JournalEntry journalEntry,
                                    @RequestParam(value = "attachment", required = false) MultipartFile attachment,
                                    Model model, RedirectAttributes redirectAttributes) {
        try {
            if (attachment != null && !attachment.isEmpty()) {
                String filePath = journalEntryService.storeAttachment(attachment);
                journalEntry.setAttachmentPath(filePath);
            }
            
            JournalEntry savedEntry = journalEntryService.submitJournalEntry(journalEntry);
            
            redirectAttributes.addFlashAttribute("message", "Journal entry submitted successfully with ID: " + savedEntry.getId());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Submission failed: " + e.getMessage());
            return "redirect:/journal/new";
        }
        return "redirect:/journal";
    }

    // View journal entry details by post reference (e.g., clicking the entry ID from the list)
    @GetMapping("/view/{id}")
    public String viewJournalEntryById(@PathVariable Long id, Model model, Authentication authentication) {
        JournalEntry entry = journalEntryService.getJournalEntryById(id);
        model.addAttribute("journalEntry", entry);
        model.addAttribute("username", authentication.getName());
        return "journal-entry-view";
    }

    // List journal entries by status (pending, approved, rejected), with optional filtering by date or search term.
    @GetMapping("/{status}")
    public String listJournalEntries(@PathVariable String status,
                                     @RequestParam(required = false) String dateFrom,
                                     @RequestParam(required = false) String dateTo,
                                     @RequestParam(required = false) String search,
                                     Model model,
                                     Authentication authentication) {
        List<JournalEntry> entries = journalEntryService.getJournalEntriesByStatus(status, dateFrom, dateTo, search);
        model.addAttribute("entries", entries);
        model.addAttribute("status", status);
        model.addAttribute("username", authentication.getName());
        return "redirect:/journal";
    }

    // Approve a journal entry (for manager)
    @PostMapping("/approve")
    public String approveJournalEntry(@RequestParam Long id) {
        journalEntryService.approveEntry(id);
        return "redirect:/journal";
    }

    // Reject a journal entry (for manager); the manager must supply a comment (reason)
    @PostMapping("/reject")
    public String rejectJournalEntry(@RequestParam Long id, @RequestParam String comment) {
        journalEntryService.rejectEntry(id, comment);
        return "journal";
    }

    @GetMapping("/general-ledger")
    public String showGeneralLedger(@RequestParam(required = false) String status,
                                    @RequestParam(required = false) String startDate,
                                    @RequestParam(required = false) String endDate,
                                    @RequestParam(required = false) String search,
                                    Model model,
                                    Authentication authentication) {
        // Pass the status filter, along with date and search parameters,
        // so only entries that match the selected status are returned.
        List<JournalEntry> entries = journalEntryService.getAllEntriesFiltered(status, startDate, endDate, search);
        model.addAttribute("entries", entries);
        // Also pass back the filter values in case you want to display them.
        model.addAttribute("status", status);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("search", search);
        model.addAttribute("username", authentication.getName());
        return "journal-general-ledger";
    }
}

package com.example.FinanceProject.controller;

import com.example.FinanceProject.entity.*;
import com.example.FinanceProject.repository.AccountRepo;
import com.example.FinanceProject.service.JournalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/journal")
public class JournalController {

    @Autowired
    private JournalService journalService;

    @Autowired
    private AccountRepo accountRepo;

    @GetMapping
    public String journalPage(Model model) {
        // Show a default list of all journal entries or just a blank page
        // We'll fetch them in the front-end with date/status filters
        return "journal";
    }

    @Secured("ROLE_USER") // or ROLE_ACCOUNTANT
    @PostMapping("/create")
    public String createEntry(
            @RequestParam("entryDate") String dateStr,
            @RequestParam("accountId") Long accountId,
            @RequestParam("debit") BigDecimal debit,
            @RequestParam("credit") BigDecimal credit,
            @RequestParam("description") String description) {

        JournalEntry entry = new JournalEntry();
        entry.setEntryDate(LocalDate.parse(dateStr));
        entry.setAccount(accountRepo.findById(accountId).orElseThrow());
        entry.setDebit(debit);
        entry.setCredit(credit);
        entry.setDescription(description);
        journalService.createEntry(entry);

        return "redirect:/journal";
    }

    // Manager Endpoints
    @Secured("ROLE_MANAGER")
    @GetMapping("/pending")
    public String pendingEntries(
            @RequestParam(required=false) String startDate,
            @RequestParam(required=false) String endDate,
            Model model) {

        LocalDate start = (startDate != null && !startDate.isEmpty()) ? LocalDate.parse(startDate) : LocalDate.MIN;
        LocalDate end = (endDate != null && !endDate.isEmpty()) ? LocalDate.parse(endDate) : LocalDate.now();

        List<JournalEntry> pending = journalService.getEntriesByStatusAndDateRange(JournalStatus.PENDING, start, end);
        model.addAttribute("entries", pending);
        model.addAttribute("status", "PENDING");
        return "journal-entries"; // a thymeleaf template to list entries
    }

    @Secured("ROLE_MANAGER")
    @PostMapping("/approve")
    public String approveEntry(@RequestParam("entryId") Long entryId) {
        journalService.approveEntry(entryId);
        return "redirect:/journal/pending";
    }

    @Secured("ROLE_MANAGER")
    @PostMapping("/reject")
    public String rejectEntry(@RequestParam("entryId") Long entryId,
                              @RequestParam("reason") String reason) {
        journalService.rejectEntry(entryId, reason);
        return "redirect:/journal/pending";
    }

    @Secured("ROLE_MANAGER")
    @GetMapping("/approved")
    public String approvedEntries(
            @RequestParam(required=false) String startDate,
            @RequestParam(required=false) String endDate,
            Model model) {

        LocalDate start = (startDate != null && !startDate.isEmpty()) ? LocalDate.parse(startDate) : LocalDate.MIN;
        LocalDate end = (endDate != null && !endDate.isEmpty()) ? LocalDate.parse(endDate) : LocalDate.now();

        List<JournalEntry> approved = journalService.getEntriesByStatusAndDateRange(JournalStatus.APPROVED, start, end);
        model.addAttribute("entries", approved);
        model.addAttribute("status", "APPROVED");
        return "journal-entries";
    }

    @Secured("ROLE_MANAGER")
    @GetMapping("/rejected")
    public String rejectedEntries(
            @RequestParam(required=false) String startDate,
            @RequestParam(required=false) String endDate,
            Model model) {

        LocalDate start = (startDate != null && !startDate.isEmpty()) ? LocalDate.parse(startDate) : LocalDate.MIN;
        LocalDate end = (endDate != null && !endDate.isEmpty()) ? LocalDate.parse(endDate) : LocalDate.now();

        List<JournalEntry> rejected = journalService.getEntriesByStatusAndDateRange(JournalStatus.REJECTED, start, end);
        model.addAttribute("entries", rejected);
        model.addAttribute("status", "REJECTED");
        return "journal-entries";
    }
}


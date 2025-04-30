// src/main/java/com/example/FinanceProject/controller/AccountController.java
package com.example.FinanceProject.controller;

import java.math.BigDecimal;
import java.util.ArrayList; // Added
import java.util.Comparator; // Added for sorting
import java.util.HashMap; // Added
import java.util.List;
import java.util.Map; // Added
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.FinanceProject.entity.Account;
import com.example.FinanceProject.entity.JournalEntry;
import com.example.FinanceProject.entity.JournalEntryLine;
import com.example.FinanceProject.entity.JournalStatus;
import com.example.FinanceProject.repository.JournalEntryLineRepo;
import com.example.FinanceProject.repository.JournalEntryRepo;
import com.example.FinanceProject.service.AccountService;
import com.example.FinanceProject.service.JournalEntryService;
import com.example.FinanceProject.service.UserService;

@Controller
@Secured({"ROLE_ADMIN", "ROLE_USER", "ROLE_MANAGER"})
@RequestMapping("/accounts")
public class AccountController {

    @Autowired
    private JournalEntryRepo journalEntryRepo;

    @Autowired
    private AccountService accountService;

    @Autowired
    private UserService userService;

    @Autowired
    private JournalEntryService journalEntryService;

    @Autowired
    private JournalEntryLineRepo journalEntryLineRepo;

    // listAccounts method remains unchanged
    @GetMapping
    public String listAccounts(
            @RequestParam(value="query", required=false) String query,
            @RequestParam(value="category", required=false) String category,
            @RequestParam(value="statement", required=false) String statement,
            @RequestParam(value="normalSide", required=false) String normalSide,
            @RequestParam(value="active", required=false) String active,
            @RequestParam(value="balanceMin", required=false) BigDecimal balanceMin,
            @RequestParam(value="balanceMax", required=false) BigDecimal balanceMax,
            @RequestParam(value="sortField", required=false, defaultValue="accountOrder") String sortField,
            @RequestParam(value="sortDir", required=false, defaultValue="asc") String sortDir,
            Model model, Authentication authentication) {
        // ... (implementation remains the same) ...
        model.addAttribute("username", authentication.getName());

        Sort sort = Sort.by(sortField);
        sort = sortDir.equalsIgnoreCase("desc") ? sort.descending() : sort.ascending();

        List<Account> accounts;
        if ((query != null && !query.trim().isEmpty()) ||
            (category != null && !category.trim().isEmpty()) ||
            (statement != null && !statement.trim().isEmpty()) ||
            (normalSide != null && !normalSide.trim().isEmpty()) ||
            (active != null && !active.trim().isEmpty()) ||
            balanceMin != null || balanceMax != null) {

            accounts = accountService.filterAccountsExtended(query, category, statement, normalSide, active, balanceMin, balanceMax, sort);
        } else {
            accounts = accountService.getAllAccounts(sort); // Fallback if no specific filter needed
        }

        accounts = accounts.stream()
                   .filter(Objects::nonNull)
                   .collect(Collectors.toList());
        model.addAttribute("accounts", accounts);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("query", query);
        model.addAttribute("category", category);
        model.addAttribute("statement", statement);
        model.addAttribute("normalSide", normalSide);
        model.addAttribute("active", active);
        model.addAttribute("balanceMin", balanceMin);
        model.addAttribute("balanceMax", balanceMax);
        return "chart-of-accounts";
    }

    // viewAccount method remains unchanged
    @GetMapping("/{id}/account-view")
    public String viewAccount(@PathVariable Long id, Model model, Authentication authentication) {
       // ... (implementation remains the same) ...
        model.addAttribute("username", authentication.getName());
        Account account = accountService.getAccountById(id);
        model.addAttribute("account", account);
        return "account-details";
    }

    // Clicking an account takes you to its ledger (req. 11)
    @GetMapping("/{id}/ledger")
    public String viewLedger(@PathVariable Long id, Model model, Authentication authentication) {
        model.addAttribute("username", authentication.getName());
        Account account = accountService.getAccountById(id);
        model.addAttribute("account", account);

        // Fetch approved entries for this account, sorted by date then entry ID
        List<JournalEntryLine> approvedLines = journalEntryLineRepo.findByAccountIdAndStatus(id, JournalStatus.APPROVED);
        // Sort lines by date and then JE ID to ensure consistent order
        approvedLines.sort(Comparator.comparing((JournalEntryLine line) -> line.getJournalEntry().getEntryDate())
                                    .thenComparing(line -> line.getJournalEntry().getId()));


        // --- Requirement 1: Running Balance Calculation ---
        List<Map<String, Object>> ledgerEntriesWithBalance = new ArrayList<>();
        BigDecimal runningBalance = account.getInitialBalance() != null ? account.getInitialBalance() : BigDecimal.ZERO;

        // Add initial balance row (optional, but good for clarity)
        // Map<String, Object> initialRow = new HashMap<>();
        // initialRow.put("date", null); // Or account creation date if available
        // initialRow.put("postRef", "Initial");
        // initialRow.put("description", "Initial Balance");
        // initialRow.put("debit", null);
        // initialRow.put("credit", null);
        // initialRow.put("balance", runningBalance);
        // initialRow.put("isInitial", true); // Add a flag for initial balance
        // ledgerEntriesWithBalance.add(initialRow);


        for (JournalEntryLine line : approvedLines) {
            BigDecimal debitChange = line.getDebit() != null ? line.getDebit() : BigDecimal.ZERO;
            BigDecimal creditChange = line.getCredit() != null ? line.getCredit() : BigDecimal.ZERO;
            BigDecimal balanceChange;

            if ("Debit".equalsIgnoreCase(account.getNormalSide())) {
                balanceChange = debitChange.subtract(creditChange);
            } else { // Credit normal side
                balanceChange = creditChange.subtract(debitChange);
            }
            runningBalance = runningBalance.add(balanceChange);

            Map<String, Object> entryMap = new HashMap<>();
            entryMap.put("date", line.getJournalEntry().getEntryDate());
            entryMap.put("postRef", line.getJournalEntry().getId()); // Link to JE view
            entryMap.put("description", line.getJournalEntry().getDescription() != null ? line.getJournalEntry().getDescription() : "");
            entryMap.put("debit", line.getDebit());
            entryMap.put("credit", line.getCredit());
            entryMap.put("balance", runningBalance); // Add the calculated running balance
            entryMap.put("journalEntryId", line.getJournalEntry().getId()); // Include JE ID for linking
            // entryMap.put("isInitial", false); // Mark as not initial balance
            ledgerEntriesWithBalance.add(entryMap);
        }
        // --- End Requirement 1 ---

        // Pass the enriched list to the model
        model.addAttribute("ledgerEntries", ledgerEntriesWithBalance);
        model.addAttribute("finalBalance", runningBalance); // Pass the final calculated balance

        return "account-ledger";
    }

    // --- Admin-only methods (unchanged) ---
    @Secured("ROLE_ADMIN")
    @GetMapping("/add")
    public String showAddAccountForm(Model model, Authentication authentication) {
       // ... (implementation remains the same) ...
        model.addAttribute("username", authentication.getName());
        model.addAttribute("account", new Account());
        return "account-add";
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/add")
    public String addAccount(@ModelAttribute("account") Account account,
                             RedirectAttributes redirectAttributes) {
       // ... (implementation remains the same) ...
         try {
            Account savedAccount = accountService.addAccount(account);
            System.out.println("Account saved with ID: " + savedAccount.getId());
            redirectAttributes.addFlashAttribute("message", "Account added successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding account: " + e.getMessage());
        }
        return "redirect:/accounts";
    }

    @Secured("ROLE_ADMIN")
    @GetMapping("/edit")
    public String showEditAccountForm(@RequestParam("id") Long id,
                                      Model model, Authentication authentication) {
       // ... (implementation remains the same) ...
        Account account = accountService.getAccountById(id);
        model.addAttribute("username", authentication.getName());
        model.addAttribute("account", account);
        return "account-edit";
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/update")
    public String updateAccount(@ModelAttribute("account") Account account,
                                RedirectAttributes redirectAttributes) {
       // ... (implementation remains the same) ...
        try {
            accountService.updateAccount(account);
            redirectAttributes.addFlashAttribute("message", "Account updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating account: " + e.getMessage());
        }
        return "redirect:/accounts";
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/deactivate")
    public String deactivateAccount(@RequestParam("id") Long id,
                                    RedirectAttributes redirectAttributes) {
       // ... (implementation remains the same) ...
         try {
            accountService.deactivateAccount(id);
            redirectAttributes.addFlashAttribute("message", "Account deactivated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deactivating account: " + e.getMessage());
        }
        return "redirect:/accounts";
    }

    // viewAccountHistory method remains unchanged but might benefit from running balance if used separately
    @GetMapping("/{id}/history")
    public String viewAccountHistory(@PathVariable Long id, Model model, Authentication authentication) {
       // ... (implementation remains the same - consider adding running balance here too if needed) ...
        Account account = accountService.getAccountById(id);
        List<JournalEntry> entries = journalEntryService.getJournalEntriesByAccountId(id);
        model.addAttribute("account", account);
        model.addAttribute("entries", entries); // Note: This passes full JEs, not lines with running balance
        model.addAttribute("username", authentication.getName()); // Added username
        return "account-ledger"; // Reuse ledger view, might need adaptation
    }
}
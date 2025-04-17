// AccountController.java
package com.example.FinanceProject.controller;

import com.example.FinanceProject.entity.Account;
import com.example.FinanceProject.entity.JournalEntry;
import com.example.FinanceProject.entity.User;
import com.example.FinanceProject.service.AccountService;
import com.example.FinanceProject.service.JournalEntryService;
import com.example.FinanceProject.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Objects;

@Controller
@Secured({"ROLE_ADMIN", "ROLE_USER", "ROLE_MANAGER"})
@RequestMapping("/accounts")
public class AccountController {
    
    @Autowired
    private AccountService accountService;

    @Autowired
    private UserService userService;

    @Autowired
    private JournalEntryService journalEntryService;
    
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

        model.addAttribute("username", authentication.getName());

        Sort sort = Sort.by(sortField);
        sort = sortDir.equalsIgnoreCase("desc") ? sort.descending() : sort.ascending();

        List<Account> accounts;
        // If any of the new parameters are provided then use the extended filter.
        if ((query != null && !query.trim().isEmpty()) ||
            (category != null && !category.trim().isEmpty()) ||
            (statement != null && !statement.trim().isEmpty()) ||
            (normalSide != null && !normalSide.trim().isEmpty()) ||
            (active != null && !active.trim().isEmpty()) ||
            balanceMin != null || balanceMax != null) {

            accounts = accountService.filterAccountsExtended(query, category, statement, normalSide, active, balanceMin, balanceMax, sort);
        } else if (query != null && !query.trim().isEmpty() && category != null && !category.trim().isEmpty()) {
            accounts = accountService.searchAndFilterAccounts(query, category, sort);
        } else if (query != null && !query.trim().isEmpty()) {
            accounts = accountService.searchAccounts(query, sort);
        } else if (category != null && !category.trim().isEmpty()) {
            accounts = accountService.filterAccountsByCategory(category, sort);
        } else {
            accounts = accountService.getAllAccounts(sort);
        }
        
        accounts = accounts.stream()
                   .filter(Objects::nonNull)
                   .collect(Collectors.toList());
        model.addAttribute("accounts", accounts);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        // Also pass back extra filter parameters so the view can pre-populate the form.
        model.addAttribute("query", query);
        model.addAttribute("category", category);
        model.addAttribute("statement", statement);
        model.addAttribute("normalSide", normalSide);
        model.addAttribute("active", active);
        model.addAttribute("balanceMin", balanceMin);
        model.addAttribute("balanceMax", balanceMax);
        return "chart-of-accounts";
    }
    
    // View individual account details (req. 7)
    @GetMapping("/{id}/account-view")
    public String viewAccount(@PathVariable Long id, Model model, Authentication authentication) {
        model.addAttribute("username", authentication.getName());
        Account account = accountService.getAccountById(id);
        model.addAttribute("account", account);
        return "account-details"; // Create a template to show detailed info
    }
    
    // Clicking an account takes you to its ledger (req. 11)
    @GetMapping("/{id}/ledger")
    public String viewLedger(@PathVariable Long id, Model model, Authentication authentication) {
        model.addAttribute("username", authentication.getName());
        Account account = accountService.getAccountById(id);
        model.addAttribute("account", account);
        model.addAttribute("ledgerEntries", new ArrayList<>());
        return "account-ledger"; // Create a template to display the account ledger
    }
    
    // Show form to add a new account (accessible only to admin)
    @Secured("ROLE_ADMIN")
    @GetMapping("/add")
    public String showAddAccountForm(Model model, Authentication authentication) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("account", new Account());
        return "account-add";
    }
    
    // Handle submission of a new account (accessible only to admin)
    @Secured("ROLE_ADMIN")
    @PostMapping("/add")
    public String addAccount(@ModelAttribute("account") Account account,
                             RedirectAttributes redirectAttributes) {
        try {
            Account savedAccount = accountService.addAccount(account);
            System.out.println("Account saved with ID: " + savedAccount.getId());
            redirectAttributes.addFlashAttribute("message", "Account added successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding account: " + e.getMessage());
        }
        return "redirect:/accounts";
    }
    
    // Show form to edit an account (accessible only to admin)
    @Secured("ROLE_ADMIN")
    @GetMapping("/edit")
    public String showEditAccountForm(@RequestParam("id") Long id,
                                      Model model, Authentication authentication) {
        Account account = accountService.getAccountById(id);
        model.addAttribute("username", authentication.getName());
        model.addAttribute("account", account);
        return "account-edit";
    }
    
    // Handle updating an account (accessible only to admin)
    @Secured("ROLE_ADMIN")
    @PostMapping("/update")
    public String updateAccount(@ModelAttribute("account") Account account,
                                RedirectAttributes redirectAttributes) {
        try {
            accountService.updateAccount(account);
            redirectAttributes.addFlashAttribute("message", "Account updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating account: " + e.getMessage());
        }
        return "redirect:/accounts";
    }
    
    // Deactivate an account (accessible only to admin)
    @Secured("ROLE_ADMIN")
    @PostMapping("/deactivate")
    public String deactivateAccount(@RequestParam("id") Long id,
                                    RedirectAttributes redirectAttributes) {
        try {
            accountService.deactivateAccount(id);
            redirectAttributes.addFlashAttribute("message", "Account deactivated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deactivating account: " + e.getMessage());
        }
        return "redirect:/accounts";
    }

    @GetMapping("/{id}/history")
    public String viewAccountHistory(@PathVariable Long id, Model model, Authentication authentication) {
        Account account = accountService.getAccountById(id);
        List<JournalEntry> entries = journalEntryService.getJournalEntriesByAccountId(id);
        model.addAttribute("account", account);
        model.addAttribute("entries", entries);
        return "account-ledger";
    }
}

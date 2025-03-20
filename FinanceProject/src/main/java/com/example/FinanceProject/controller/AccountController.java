// AccountController.java
package com.example.FinanceProject.controller;

import com.example.FinanceProject.Account;
import com.example.FinanceProject.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@Secured({"ROLE_ADMIN", "ROLE_USER"})  // Accessible to both roles
@RequestMapping("/accounts")
public class AccountController {
    
    @Autowired
    private AccountService accountService;
    
    // List all accounts (chart of accounts with optional search/filter)
    @GetMapping
    public String listAccounts(@RequestParam(value="query", required=false) String query,
                               Model model, Authentication authentication) {
        model.addAttribute("username", authentication.getName()); // Requirement 9
        
        List<Account> accounts;
        if (query != null && !query.trim().isEmpty()) {
            accounts = accountService.searchAccounts(query);
        } else {
            accounts = accountService.getAllAccounts();
        }
        model.addAttribute("accounts", accounts);
        return "account-list";  // Your Thymeleaf template should include the logo (req. 10),
                                // filter options (req. 12), calendar (req. 13), and navigation buttons (req. 14)
    }
    
    // View individual account details (req. 7)
    @GetMapping("/{id}")
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
        // For demonstration, assume you have a ledgerService to fetch ledger entries:
        // model.addAttribute("ledgerEntries", ledgerService.getEntriesByAccountId(id));
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
            accountService.addAccount(account);
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
}

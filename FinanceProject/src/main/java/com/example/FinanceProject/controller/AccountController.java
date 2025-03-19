package com.example.FinanceProject.controller;

import com.example.FinanceProject.Account;
import com.example.FinanceProject.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Secured({"ROLE_ADMIN", "ROLE_USER"})  // Allow both admin and user roles to access these endpoints
@RequestMapping("/accounts")
public class AccountController {
    
    @Autowired
    private AccountService accountService;
    
    // List all accounts
    @GetMapping
    public String listAccounts(Model model) {
        model.addAttribute("accounts", accountService.getAllAccounts());
        return "account-list";
    }
    
    // Show form to add a new account
    @GetMapping("/add")
    public String showAddAccountForm(Model model) {
        model.addAttribute("account", new Account());
        return "account-add";
    }
    
    // Handle submission of a new account
    @PostMapping("/add")
    public String addAccount(@ModelAttribute("account") Account account, RedirectAttributes redirectAttributes) {
        try {
            accountService.addAccount(account);
            redirectAttributes.addFlashAttribute("message", "Account added successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding account: " + e.getMessage());
        }
        return "redirect:/user/accounts";
    }
    
    // Show form to edit an account
    @GetMapping("/edit")
    public String showEditAccountForm(@RequestParam("id") Long id, Model model) {
        Account account = accountService.getAccountById(id);
        model.addAttribute("account", account);
        return "account-edit";
    }
    
    // Handle updating an account
    @PostMapping("/update")
    public String updateAccount(@ModelAttribute("account") Account account, RedirectAttributes redirectAttributes) {
        try {
            accountService.updateAccount(account);
            redirectAttributes.addFlashAttribute("message", "Account updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating account: " + e.getMessage());
        }
        return "redirect:/user/accounts";
    }
    
    // Deactivate an account
    @PostMapping("/deactivate")
    public String deactivateAccount(@RequestParam("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            accountService.deactivateAccount(id);
            redirectAttributes.addFlashAttribute("message", "Account deactivated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deactivating account: " + e.getMessage());
        }
        return "redirect:/user/accounts";
    }
}


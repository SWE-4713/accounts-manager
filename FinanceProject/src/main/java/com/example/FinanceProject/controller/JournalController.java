package com.example.FinanceProject.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class JournalController {

    @GetMapping("/journal")
    public String journalPage(Model model, Authentication authentication) {
        // Add the logged-in user's name to the model for display in the header.
        model.addAttribute("username", authentication.getName());
        // Return the view name for journalizing page (journal.html)
        return "journal";
    }
}

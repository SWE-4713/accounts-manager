package com.example.FinanceProject.controller;

import com.example.FinanceProject.service.EventLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class EventLogController {

    @Autowired
    private EventLogService eventLogService;
    
    @GetMapping("/event-logs")
    public String eventLogsPage(@RequestParam(value = "tab", defaultValue = "accounts") String tab,
                                Model model, Authentication authentication) {
        // add the logged-in user's name
        model.addAttribute("username", authentication.getName());
        model.addAttribute("activeTab", tab);
        // Based on the tab, filter event logs:
        if ("journal".equals(tab)) {
            model.addAttribute("eventLogs", eventLogService.getJournalEventLogs());
        } else {
            model.addAttribute("eventLogs", eventLogService.getAccountEventLogs());
        }
        return "event-logs";
    }
}

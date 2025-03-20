package com.example.FinanceProject.controller;

import com.example.FinanceProject.service.EventLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class EventLogController {

    @Autowired
    private EventLogService eventLogService;
    
    @GetMapping("/event-logs")
    public String eventLogsPage(Model model, Authentication authentication) {
        // Add the logged-in user's name to the model for display in the header.
        model.addAttribute("username", authentication.getName());
        // Add all event logs to the model (you might need to create a method in EventLogService to fetch all logs)
        model.addAttribute("eventLogs", eventLogService.getAllEventLogs());
        // Return the view name for event logs page (event-logs.html)
        return "event-logs";
    }
}

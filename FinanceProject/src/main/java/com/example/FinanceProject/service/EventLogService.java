package com.example.FinanceProject.service;

import com.example.FinanceProject.entity.EventLog;
import com.example.FinanceProject.entity.JournalEntry;
import com.example.FinanceProject.repository.EventLogRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventLogService {

    @Autowired
    private EventLogRepo eventLogRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    // Existing method for accounts (kept for backward compatibility)
    public void logEvent(Object before, Object after, Long userId, String action) {
        EventLog log = new EventLog();
        log.setUserId(userId);
        log.setTimestamp(LocalDateTime.now());
        log.setAction(action);
        try {
            if (before != null) {
                log.setBeforeImage(objectMapper.writeValueAsString(before));
            }
            if (after != null) {
                log.setAfterImage(objectMapper.writeValueAsString(after));
            }
        } catch (Exception e) {
            log.setBeforeImage(before != null ? before.toString() : null);
            log.setAfterImage(after != null ? after.toString() : null);
        }
        eventLogRepository.save(log);
    }

    // New method to log journal entry events (for creation, approval, rejection, etc.)
    public void logJournalEvent(JournalEntry journalEntry, Long userId, String action) {
        EventLog log = new EventLog();
        log.setUserId(userId);
        log.setTimestamp(LocalDateTime.now());
        log.setAction(action);
        try {
            log.setAfterImage(objectMapper.writeValueAsString(journalEntry));
        } catch (Exception e) {
            log.setAfterImage(journalEntry.toString());
        }
        // No "before" image on creation
        eventLogRepository.save(log);
    }

    public List<EventLog> getAllEventLogs() {
        return eventLogRepository.findAll();
    }

    // Existing filtering methods
    public List<EventLog> getAccountEventLogs() {
        return eventLogRepository.findAll().stream()
            .filter(log -> {
                String act = log.getAction();
                return "ADD".equals(act) || "MODIFY".equals(act) || "DEACTIVATE".equals(act);
            })
            .collect(Collectors.toList());
    }

    public List<EventLog> getJournalEventLogs() {
        return eventLogRepository.findAll().stream()
            .filter(log -> {
                String act = log.getAction();
                return "CREATE".equals(act) || "APPROVE".equals(act) || "REJECT".equals(act);
            })
            .collect(Collectors.toList());
    }
}

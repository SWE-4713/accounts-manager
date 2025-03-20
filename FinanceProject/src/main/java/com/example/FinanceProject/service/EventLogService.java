// EventLogService.java
package com.example.FinanceProject.service;

import com.example.FinanceProject.Account;
import com.example.FinanceProject.EventLog;
import com.example.FinanceProject.repository.EventLogRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

@Service
public class EventLogService {

    @Autowired
    private EventLogRepo eventLogRepository;
    
    // Using Jackson to convert objects to JSON strings for a clearer before/after view.
    private ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Log an event.
     * @param before The state before change (can be null for new records)
     * @param after The state after change (can be null if deletion)
     * @param userId ID of the user making the change
     * @param action The action performed ("ADD", "MODIFY", "DEACTIVATE")
     */
    public void logEvent(Account before, Account after, Long userId, String action) {
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
            // In case of serialization error, fall back to simple toString() values
            log.setBeforeImage(before != null ? before.toString() : null);
            log.setAfterImage(after != null ? after.toString() : null);
        }
        eventLogRepository.save(log);
    }

    
    public List<EventLog> getAllEventLogs() {
        return eventLogRepository.findAll();
    }
}

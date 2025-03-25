// EventLogRepository.java
package com.example.FinanceProject.repository;

import com.example.FinanceProject.entity.EventLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventLogRepo extends JpaRepository<EventLog, Long> {
    // Additional query methods can be added if needed.
}

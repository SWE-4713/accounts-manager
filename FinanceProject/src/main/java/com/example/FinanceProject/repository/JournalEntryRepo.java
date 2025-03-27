package com.example.FinanceProject.repository;

import com.example.FinanceProject.entity.JournalEntry;
import com.example.FinanceProject.entity.JournalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface JournalEntryRepo extends JpaRepository<JournalEntry, Long> {

    // Find by status
    List<JournalEntry> findByStatus(JournalStatus status);

    // Filter by date range and status
    List<JournalEntry> findByStatusAndEntryDateBetween(JournalStatus status, LocalDate start, LocalDate end);

    // Possibly more methods as needed
}

package com.example.FinanceProject.repository;

import com.example.FinanceProject.entity.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

public interface JournalEntryRepo extends JpaRepository<JournalEntry, Long> {
    List<JournalEntry> findByStatus(String status);
    List<JournalEntry> findByEntryDateBetween(LocalDateTime start, LocalDateTime end);
    Optional<JournalEntry> findByPostReference(String postReference);
    // Additional search methods by account name or amount can be defined using @Query
}

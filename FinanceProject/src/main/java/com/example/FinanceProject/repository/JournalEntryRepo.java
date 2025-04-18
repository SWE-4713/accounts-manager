package com.example.FinanceProject.repository;

import com.example.FinanceProject.entity.JournalEntry;
import com.example.FinanceProject.entity.JournalStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public interface JournalEntryRepo extends JpaRepository<JournalEntry, Long> {
    List<JournalEntry> findByStatus(JournalStatus statusEnum);
    List<JournalEntry> findByEntryDateBetween(LocalDateTime start, LocalDateTime end);
    // Additional search methods by account name or amount can be defined using @Query
    List<JournalEntry> findByStatusAndEntryDateBetween(JournalStatus status, LocalDate start, LocalDate end);
    @Query("SELECT DISTINCT je FROM JournalEntry je JOIN je.lines line WHERE line.account.id = :accountId")
    List<JournalEntry> findByAccountId(@Param("accountId") Long accountId);
}

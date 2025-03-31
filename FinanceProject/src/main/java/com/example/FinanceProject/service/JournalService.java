package com.example.FinanceProject.service;

import com.example.FinanceProject.entity.*;
import com.example.FinanceProject.repository.JournalEntryRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class JournalService {

    @Autowired
    private JournalEntryRepo journalEntryRepo;

    @Autowired
    private AccountService accountService; // for updating ledger

    public JournalEntry createEntry(JournalEntry entry) {
        entry.setStatus(JournalStatus.PENDING);
        return journalEntryRepo.save(entry);
    }

    public List<JournalEntry> getEntriesByStatus(JournalStatus status) {
        return journalEntryRepo.findByStatus(status);
    }

    public List<JournalEntry> getEntriesByStatusAndDateRange(JournalStatus status, LocalDate start, LocalDate end) {
        return journalEntryRepo.findByStatusAndEntryDateBetween(status, start, end);
    }

    public void approveEntry(Long id) {
        JournalEntry entry = journalEntryRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Journal entry not found"));
        entry.setStatus(JournalStatus.APPROVED);
        journalEntryRepo.save(entry);

        // Reflect in ledger
        accountService.updateLedgerForApprovedEntry(entry);
    }

    public void rejectEntry(Long id, String reason) {
        JournalEntry entry = journalEntryRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Journal entry not found"));
        entry.setStatus(JournalStatus.REJECTED);
        entry.setComment(reason);
        journalEntryRepo.save(entry);
    }
}

package com.example.FinanceProject.service;

import com.example.FinanceProject.entity.ErrorsDatabase;
import com.example.FinanceProject.entity.JournalEntry;
import com.example.FinanceProject.entity.JournalEntryLine;
import com.example.FinanceProject.repository.ErrorDatabaseRepo;
import com.example.FinanceProject.repository.JournalEntryRepo;
import com.example.FinanceProject.repository.AccountRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

@Service
public class JournalEntryService {

    @Autowired
    private JournalEntryRepo journalEntryRepository;

    @Autowired
    private AccountRepo accountRepo;

    @Autowired
    private ErrorDatabaseRepo errorsDatabaseRepository; // Repository for errors

    public JournalEntry submitJournalEntry(JournalEntry entry) {
        // Validate: each entry must have at least one debit and one credit, and total debits must equal total credits.
        BigDecimal totalDebit = entry.getLines().stream()
                .map(JournalEntryLine::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = entry.getLines().stream()
                .map(JournalEntryLine::getCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebit.compareTo(BigDecimal.ZERO) <= 0 || totalCredit.compareTo(BigDecimal.ZERO) <= 0) {
            saveError("Each journal entry must have at least one debit and one credit.");
            throw new IllegalArgumentException("Each journal entry must have at least one debit and one credit.");
        }

        if (totalDebit.compareTo(totalCredit) != 0) {
            saveError("Total debits (" + totalDebit + ") do not equal total credits (" + totalCredit + ").");
            throw new IllegalArgumentException("Total debits do not equal total credits.");
        }

        // Generate a post reference – for simplicity, using the journal entry ID after saving.
        JournalEntry saved = journalEntryRepository.save(entry);
        saved.setPostReference("PR" + saved.getId());
        return journalEntryRepository.save(saved);
    }

    private void saveError(String description) {
        // Create and save an error record in the ErrorsDatabase
        ErrorsDatabase error = new ErrorsDatabase();
        error.setDescription(description);
        errorsDatabaseRepository.save(error);
    }

    // Methods for manager to approve/reject
    public JournalEntry approveEntry(Long id) {
        JournalEntry entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Journal entry not found."));
        entry.setStatus("APPROVED");
        return journalEntryRepository.save(entry);
    }

    public JournalEntry rejectEntry(Long id, String comment) {
        JournalEntry entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Journal entry not found."));
        entry.setStatus("REJECTED");
        entry.setManagerComment(comment);
        return journalEntryRepository.save(entry);
    }

    // Dummy implementation of file attachment storage
    public String storeAttachment(MultipartFile file) throws IOException {
        // In production, use a robust file storage mechanism
        String uploadsDir = "/uploads/journal/";
        File dir = new File(uploadsDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        String filePath = uploadsDir + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        file.transferTo(new File(filePath));
        return filePath;
    }

    // Retrieve journal entries by status with optional filtering (dummy implementation)
    public List<JournalEntry> getJournalEntriesByStatus(String status, String dateFrom, String dateTo, String search) {
        // Implement filtering by date or search term as needed. For now, simply:
        return journalEntryRepository.findByStatus(status);
    }

    public JournalEntry getJournalEntryByPostReference(String postReference) {
        // Implement a lookup by postReference (assuming uniqueness)
        return journalEntryRepository.findByPostReference(postReference)
                .orElseThrow(() -> new IllegalArgumentException("Journal entry not found"));
    }

    public List<JournalEntry> findByStatus(String status) {
        return journalEntryRepository.findByStatus(status);
    }
}

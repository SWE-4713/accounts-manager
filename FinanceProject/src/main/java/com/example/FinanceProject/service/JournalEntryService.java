package com.example.FinanceProject.service;

import com.example.FinanceProject.entity.Account;
import com.example.FinanceProject.entity.ErrorsDatabase;
import com.example.FinanceProject.entity.JournalEntry;
import com.example.FinanceProject.entity.JournalEntryLine;
import com.example.FinanceProject.entity.JournalStatus;
import com.example.FinanceProject.repository.ErrorDatabaseRepo;
import com.example.FinanceProject.repository.JournalEntryRepo;
import com.example.FinanceProject.repository.AccountRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.FinanceProject.entity.JournalEntry;
import com.example.FinanceProject.entity.JournalStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.multipart.MultipartFile;

@Service
public class JournalEntryService {

    @Autowired
    private JournalEntryRepo journalEntryRepository;

    @Autowired
    private AccountRepo accountRepo;

    @Autowired
    private ErrorDatabaseRepo errorsDatabaseRepository; // Repository for errors

    @Autowired
    private AccountService accountService;
    
    // JournalEntryService.java
    public List<JournalEntry> submitJournalEntry(JournalEntry entry) {
        // Calculate total debits and credits from the submitted lines
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for (JournalEntryLine line : entry.getLines()) {
            totalDebit = totalDebit.add(line.getDebit() != null ? line.getDebit() : BigDecimal.ZERO);
            totalCredit = totalCredit.add(line.getCredit() != null ? line.getCredit() : BigDecimal.ZERO);
        }
        
        // Validate that at least one debit and one credit is provided.
        if (totalDebit.compareTo(BigDecimal.ZERO) <= 0 || totalCredit.compareTo(BigDecimal.ZERO) <= 0) {
            saveError("Each journal entry must have at least one debit and one credit.");
            throw new IllegalArgumentException("Each journal entry must have at least one debit and one credit.");
        }
        
        // Validate that the total debits equal the total credits.
        if (totalDebit.compareTo(totalCredit) != 0) {
            saveError("Total debits (" + totalDebit + ") do not equal total credits (" + totalCredit + ").");
            throw new IllegalArgumentException("Total debits do not equal total credits.");
        }

        List<JournalEntry> savedEntries = new ArrayList<>();

        // Process each line as an independent journal entry.
        for (JournalEntryLine line : entry.getLines()) {
            // Validate that an account is provided.
            if (line.getAccountId() == null) {
                throw new IllegalArgumentException("Account is required for each line.");
            }
            Account account = accountService.getAccountById(line.getAccountId());
            if (account == null) {
                throw new IllegalArgumentException("Account not found for ID: " + line.getAccountId());
            }

            // Create a new JournalEntry record for the line.
            JournalEntry newEntry = new JournalEntry();
            newEntry.setEntryDate(entry.getEntryDate());              // Copy header entry date
            newEntry.setDescription(entry.getDescription());            // Copy description if available
            newEntry.setStatus(JournalStatus.PENDING);                  // Set default status
            newEntry.setAttachmentPath(entry.getAttachmentPath());      // Copy attachment if provided
            newEntry.setDebit(line.getDebit());                         // Set debit from the line
            newEntry.setCredit(line.getCredit());                       // Set credit from the line
            newEntry.setAccount(account);                               // Associate the account

            // Save the new entry and set a post reference.
            JournalEntry savedEntry = journalEntryRepository.save(newEntry);
            savedEntry.setPostReference("PR" + savedEntry.getId());
            savedEntry = journalEntryRepository.save(savedEntry);
            savedEntries.add(savedEntry);
        }
        return savedEntries;
    }

    private void saveError(String description) {
        // Create and save an error record in the ErrorsDatabase
        ErrorsDatabase error = new ErrorsDatabase();
        error.setErrorDescription(description);
        errorsDatabaseRepository.save(error);
    }

    // Methods for manager to approve/reject
    public JournalEntry approveEntry(Long id) {
        JournalEntry entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Journal entry not found."));
        entry.setStatus(JournalStatus.APPROVED);
        return journalEntryRepository.save(entry);
    }

    public JournalEntry rejectEntry(Long id, String comment) {
        JournalEntry entry = journalEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Journal entry not found."));
        entry.setStatus(JournalStatus.REJECTED);
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
        JournalStatus statusEnum = JournalStatus.valueOf(status.toUpperCase());
        return journalEntryRepository.findByStatus(statusEnum);
    }

    public JournalEntry getJournalEntryByPostReference(String postReference) {
        // Implement a lookup by postReference (assuming uniqueness)
        return journalEntryRepository.findByPostReference(postReference)
                .orElseThrow(() -> new IllegalArgumentException("Journal entry not found"));
    }

    public List<JournalEntry> findByStatus(String status) {
        JournalStatus statusEnum = JournalStatus.valueOf(status.toUpperCase());
        return journalEntryRepository.findByStatus(statusEnum);
    }

    public List<JournalEntry> getAllEntriesFiltered(String status, String startDate, String endDate, String search) {
        JournalStatus journalStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                journalStatus = JournalStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // If status is invalid, leave journalStatus as null to ignore filtering by status
            }
        }
        
        LocalDate start = (startDate != null && !startDate.isEmpty()) ? LocalDate.parse(startDate) : LocalDate.MIN;
        LocalDate end = (endDate != null && !endDate.isEmpty()) ? LocalDate.parse(endDate) : LocalDate.now();

        List<JournalEntry> entries;
        if (journalStatus != null) {
            entries = journalEntryRepository.findByStatusAndEntryDateBetween(journalStatus, start, end);
        } else {
            entries = journalEntryRepository.findAll().stream()
                    .filter(e -> !e.getEntryDate().isBefore(start) && !e.getEntryDate().isAfter(end))
                    .collect(Collectors.toList());
        }

        if (search != null && !search.trim().isEmpty()) {
            String lowerSearch = search.toLowerCase();
            entries = entries.stream().filter(e -> 
                (e.getLines().stream().anyMatch(line -> 
                    line.getAccount() != null && 
                    line.getAccount().getAccountName().toLowerCase().contains(lowerSearch)))
                || (e.getDebit() != null && e.getDebit().toString().contains(lowerSearch))
                || (e.getCredit() != null && e.getCredit().toString().contains(lowerSearch))
                || (e.getEntryDate() != null && e.getEntryDate().toString().contains(lowerSearch))
            ).collect(Collectors.toList());
        }
        return entries;
    }
}

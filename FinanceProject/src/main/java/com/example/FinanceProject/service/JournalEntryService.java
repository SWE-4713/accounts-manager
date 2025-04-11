package com.example.FinanceProject.service;

import com.example.FinanceProject.entity.JournalEntry;
import com.example.FinanceProject.entity.JournalEntryLine;
import com.example.FinanceProject.entity.JournalStatus;
import com.example.FinanceProject.entity.Account;
import com.example.FinanceProject.entity.ErrorsDatabase;
import com.example.FinanceProject.repository.JournalEntryRepo;
import com.example.FinanceProject.repository.AccountRepo;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.FinanceProject.repository.ErrorDatabaseRepo;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.web.multipart.MultipartFile;

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

    @Autowired
    private EventLogService eventLogService;
    
    public JournalEntry submitJournalEntry(JournalEntry entry) {
        if(entry.getLines() == null || entry.getLines().isEmpty()){
            throw new IllegalArgumentException("At least one journal entry line is required.");
        }
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        for(JournalEntryLine line : entry.getLines()){
            if (line.getAccount() == null) {
                Long accountId = line.getAccountId();  
                if (accountId == null) {
                    throw new IllegalArgumentException("Each line must have an account selected.");
                }
                Account account = accountService.getAccountById(accountId);
                if (account == null) {
                    throw new IllegalArgumentException("Account not found for id: " + accountId);
                }
                line.setAccount(account);
            }
            BigDecimal debit = line.getDebit() != null ? line.getDebit() : BigDecimal.ZERO;
            BigDecimal credit = line.getCredit() != null ? line.getCredit() : BigDecimal.ZERO;
            if(debit.compareTo(BigDecimal.ZERO) > 0 && credit.compareTo(BigDecimal.ZERO) > 0){
                throw new IllegalArgumentException("A line cannot have both a debit and a credit amount.");
            }
            totalDebit = totalDebit.add(debit);
            totalCredit = totalCredit.add(credit);
            line.setJournalEntry(entry);
        }
        if(totalDebit.compareTo(totalCredit) != 0){
            throw new IllegalArgumentException("Total debits must equal total credits.");
        }
        entry.setStatus(JournalStatus.PENDING);

        // Capture the current username (and ideally, convert to a numeric userId)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && !auth.getName().equals("anonymousUser")) ? auth.getName() : "system";
        entry.setCreatedBy(username);

        JournalEntry saved = journalEntryRepository.save(entry);
        saved.setDescription(entry.getDescription());
        
        // Log the creation event for the journal entry.
        // Replace 0L with actual user id if available.
        eventLogService.logJournalEvent(saved, 0L, "CREATE");
        
        return saved;
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

    public String storeAttachment(MultipartFile file) throws IOException {
        // Check allowed file types here, e.g., by MIME type or extension.
        String[] allowedExtensions = {"pdf", "doc", "docx", "xls", "xlsx", "csv", "jpg", "jpeg", "png"};
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            boolean allowed = false;
            for(String allowedExt : allowedExtensions){
                if(allowedExt.equals(ext)){
                    allowed = true;
                    break;
                }
            }
            if(!allowed) {
                throw new IllegalArgumentException("File type not allowed.");
            }
        }

        String uploadsDir = "/uploads/journal/"; // Adjust your storage path as needed
        File dir = new File(uploadsDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String filePath = uploadsDir + System.currentTimeMillis() + "_" + originalFilename;
        file.transferTo(new File(filePath));
        return filePath;
    }

    // Retrieve journal entries by status with optional filtering (dummy implementation)
    public List<JournalEntry> getJournalEntriesByStatus(String status, String dateFrom, String dateTo, String search) {
        JournalStatus statusEnum = JournalStatus.valueOf(status.toUpperCase());
        return journalEntryRepository.findByStatus(statusEnum);
    }

    public List<JournalEntry> findByStatus(String status) {
        JournalStatus statusEnum = JournalStatus.valueOf(status.toUpperCase());
        return journalEntryRepository.findByStatus(statusEnum);
    }

    public List<JournalEntry> getAllEntriesFiltered(String status, String startDate, String endDate, String search) {
        // Process status filter: if provided, parse into JournalStatus.
        JournalStatus journalStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                journalStatus = JournalStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // If status is invalid, leave journalStatus as null to ignore filtering by status.
            }
        }
        
        // Process date filtering.
        LocalDate start = (startDate != null && !startDate.isEmpty()) ? LocalDate.parse(startDate) : LocalDate.MIN;
        LocalDate end = (endDate != null && !endDate.isEmpty()) ? LocalDate.parse(endDate) : LocalDate.now();
        
        // Retrieve entries filtered by status and date.
        List<JournalEntry> entries;
        if (journalStatus != null) {
            entries = journalEntryRepository.findByStatusAndEntryDateBetween(journalStatus, start, end);
        } else {
            entries = journalEntryRepository.findAll().stream()
                        .filter(e -> !e.getEntryDate().isBefore(start) && !e.getEntryDate().isAfter(end))
                        .collect(Collectors.toList());
        }
        
        // If a search term is provided, filter entries based on description,
        // account name (from its lines), debit and credit amounts, and entry date.
        if (search != null && !search.trim().isEmpty()) {
            String lowerSearch = search.toLowerCase().replaceAll("\\s+", ""); // remove spaces in search term
            entries = entries.stream().filter(e ->
                // Check description field ignoring spaces.
                (e.getDescription() != null && e.getDescription().replaceAll("\\s+", "").toLowerCase().contains(lowerSearch))
                ||
                // Check account name from journal lines.
                (e.getLines().stream().anyMatch(line ->
                    line.getAccount() != null &&
                    line.getAccount().getAccountName().replaceAll("\\s+", "").toLowerCase().contains(lowerSearch)
                ))
                ||
                // Check debit amount.
                (e.getTotalDebit() != null && e.getTotalDebit().toString().contains(lowerSearch))
                ||
                // Check credit amount.
                (e.getTotalCredit() != null && e.getTotalCredit().toString().contains(lowerSearch))
                ||
                // Check entry date (as string).
                (e.getEntryDate() != null && e.getEntryDate().toString().contains(lowerSearch))
            ).collect(Collectors.toList());
        }
        return entries;
    }

    public JournalEntry getJournalEntryById(Long id) {
        return journalEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Journal entry not found with id: " + id));
    }

    public List<JournalEntry> findAllJournalEntries() {
        return journalEntryRepository.findAll();
    }

    public List<JournalEntry> getJournalEntriesByAccountId(Long accountId) {
        return journalEntryRepository.findAll().stream()
           .filter(entry -> entry.getLines().stream()
               .anyMatch(line -> line.getAccount() != null && line.getAccount().getId().equals(accountId))
           ).collect(Collectors.toList());
    }
    
}

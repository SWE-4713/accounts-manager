// src/main/java/com/example/FinanceProject/service/JournalEntryService.java
package com.example.FinanceProject.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.HashSet; // Added for checking duplicates
import java.util.List;
import java.util.Set; // Added for checking duplicates
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.FinanceProject.entity.Account;
import com.example.FinanceProject.entity.ErrorsDatabase;
import com.example.FinanceProject.entity.JournalEntry;
import com.example.FinanceProject.entity.JournalEntryLine;
import com.example.FinanceProject.entity.JournalStatus;
import com.example.FinanceProject.repository.AccountRepo;
import com.example.FinanceProject.repository.ErrorDatabaseRepo;
import com.example.FinanceProject.repository.JournalEntryRepo;

import jakarta.transaction.Transactional;

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

    @Transactional // Make submission transactional
    public JournalEntry submitJournalEntry(JournalEntry entry) {
        if (entry.getLines() == null || entry.getLines().isEmpty()) {
            throw new IllegalArgumentException("At least one journal entry line is required.");
        }
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        Set<Long> usedAccountIds = new HashSet<>(); // Added to track used accounts

        for (JournalEntryLine line : entry.getLines()) {
            if (line.getAccount() == null) {
                Long accountId = line.getAccountId();
                if (accountId == null) {
                    throw new IllegalArgumentException("Each line must have an account selected.");
                }
                Account account = accountService.getAccountById(accountId); // Use service to find account
                // No need to check for null here as getAccountById throws if not found
                line.setAccount(account);
            }

            // --- Requirement: Unique Account per JE ---
            Long currentAccountId = line.getAccount().getId();
            if (!usedAccountIds.add(currentAccountId)) { // .add() returns false if element already exists
                throw new IllegalArgumentException("Account '" + line.getAccount().getAccountName() + "' cannot be used more than once in the same journal entry.");
            }
            // --- End Requirement ---

            BigDecimal debit = line.getDebit() != null ? line.getDebit() : BigDecimal.ZERO;
            BigDecimal credit = line.getCredit() != null ? line.getCredit() : BigDecimal.ZERO;

            if (debit.compareTo(BigDecimal.ZERO) < 0 || credit.compareTo(BigDecimal.ZERO) < 0) {
               throw new IllegalArgumentException("Debit and credit amounts cannot be negative.");
            }

            if (debit.compareTo(BigDecimal.ZERO) > 0 && credit.compareTo(BigDecimal.ZERO) > 0) {
                throw new IllegalArgumentException("A line cannot have both a debit and a credit amount.");
            }
            if (debit.compareTo(BigDecimal.ZERO) == 0 && credit.compareTo(BigDecimal.ZERO) == 0) {
               throw new IllegalArgumentException("A line must have either a debit or a credit amount.");
            }

            totalDebit = totalDebit.add(debit);
            totalCredit = totalCredit.add(credit);
            line.setJournalEntry(entry); // Ensure back-reference is set
        }

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new IllegalArgumentException("Total debits (" + totalDebit + ") must equal total credits (" + totalCredit + ").");
        }

        entry.setStatus(JournalStatus.PENDING);

        // Capture the current username
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && !auth.getName().equals("anonymousUser")) ? auth.getName() : "system";
        entry.setCreatedBy(username);

        JournalEntry saved = journalEntryRepository.save(entry);

        // Log the creation event
        eventLogService.logJournalEvent(null, saved, 0L, "CREATE"); // Replace 0L if user ID available

        return saved;
    }


    private void saveError(String description) {
        ErrorsDatabase error = new ErrorsDatabase();
        error.setErrorDescription(description);
        errorsDatabaseRepository.save(error);
    }

    public JournalEntry rejectEntry(Long id, String comment) {
        JournalEntry existing = journalEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Journal entry not found."));

        // Capture before state
        JournalEntry beforeUpdate = deepCopyJournalEntry(existing);

        existing.setStatus(JournalStatus.REJECTED);
        existing.setManagerComment(comment);
        JournalEntry updated = journalEntryRepository.save(existing);

        // Log event
        eventLogService.logJournalEvent(beforeUpdate, updated, 0L, "REJECT"); // Replace 0L if user ID available
        return updated;
    }

    @Transactional // Ensure atomicity
    public JournalEntry approveEntry(Long id) {
        JournalEntry existing = journalEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Journal entry not found."));

        if (existing.getStatus() == JournalStatus.APPROVED) {
             throw new IllegalStateException("Journal entry is already approved.");
        }

        // Capture the before snapshot
        JournalEntry beforeUpdate = deepCopyJournalEntry(existing);

        // Update status to APPROVED *before* processing lines
        existing.setStatus(JournalStatus.APPROVED);
        JournalEntry updatedEntry = journalEntryRepository.save(existing); // Save the status change first

        // --- Requirement 1: Update Account Balances ---
        for (JournalEntryLine line : updatedEntry.getLines()) { // Iterate using the updated entry
            Account acct = accountRepo.findById(line.getAccount().getId())
                .orElseThrow(() -> new RuntimeException("Account not found during approval: " + line.getAccount().getId()));

            BigDecimal debitChange  = line.getDebit() != null  ? line.getDebit()  : BigDecimal.ZERO;
            BigDecimal creditChange = line.getCredit() != null ? line.getCredit() : BigDecimal.ZERO;

            // Update total lifetime debits/credits for the account
            acct.setDebit(acct.getDebit().add(debitChange));
            acct.setCredit(acct.getCredit().add(creditChange));

            // Calculate the change in balance based on this entry's line and the account's normal side
            BigDecimal balanceChange;
            if ("Debit".equalsIgnoreCase(acct.getNormalSide())) {
                balanceChange = debitChange.subtract(creditChange);
            } else { // Normal side is Credit
                balanceChange = creditChange.subtract(debitChange);
            }

            // Update the account balance by *adding* the calculated change
            acct.setBalance(acct.getBalance().add(balanceChange));

            // Save the updated account *within the loop* to ensure each account's change is persisted
            accountRepo.save(acct);
            // Ledger Update: Handled implicitly by saving the JournalEntry with its lines.
            // The JournalEntryLine itself acts as the ledger detail for this transaction.
        }
        // --- End Requirement 1 Change ---

        //Log event with action "APPROVE"
        eventLogService.logJournalEvent(beforeUpdate, updatedEntry, 0L, "APPROVE"); // Assuming 0L for system/manager approval
        return updatedEntry; // Return the entry with the updated status
    }

    // Helper method for deep copying JournalEntry for logging (Unchanged)
    private JournalEntry deepCopyJournalEntry(JournalEntry original) {
        // ... (implementation remains the same) ...
         JournalEntry copy = new JournalEntry();
         copy.setId(original.getId());
         copy.setEntryDate(original.getEntryDate());
         copy.setStatus(original.getStatus());
         copy.setDescription(original.getDescription());
         copy.setEntryComment(original.getEntryComment());
         copy.setAttachmentPath(original.getAttachmentPath());
         copy.setCreatedBy(original.getCreatedBy());
         copy.setType(original.getType());
         copy.setManagerComment(original.getManagerComment());
         List<JournalEntryLine> linesCopy = original.getLines().stream().map(line -> {
             JournalEntryLine lineCopy = new JournalEntryLine();
             lineCopy.setId(line.getId());
             lineCopy.setDebit(line.getDebit());
             lineCopy.setCredit(line.getCredit());
             if (line.getAccount() != null) {
                 Account accRef = new Account();
                 accRef.setId(line.getAccount().getId());
                 accRef.setAccountName(line.getAccount().getAccountName());
                 accRef.setAccountNumber(line.getAccount().getAccountNumber());
                 lineCopy.setAccount(accRef);
             }
             return lineCopy;
         }).collect(Collectors.toList());
         copy.setLines(linesCopy);
         return copy;
    }


    @Value("${app.upload.dir}")
    private String uploadRootDir;

    // storeAttachment method remains unchanged
    public String storeAttachment(MultipartFile file) throws IOException {
        // ... (implementation remains the same) ...
         // Check allowed file types
        String[] allowedExtensions = {"pdf", "doc", "docx", "xls", "xlsx", "csv", "jpg", "jpeg", "png"};
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("Original filename is null");
        }

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

        Path uploadDirectory = Paths.get(System.getProperty("user.dir"), "app_uploads", "journal").toAbsolutePath();
        System.out.println("Attempting to create directory: " + uploadDirectory);

        try {
            Files.createDirectories(uploadDirectory);
        } catch (IOException e) {
            System.err.println("Failed to create directory: " + e.getMessage());
            throw new IOException("Could not create upload directory", e);
        }

        if (!Files.exists(uploadDirectory)) {
            throw new IOException("Failed to create directory: " + uploadDirectory);
        }

        String filename = System.currentTimeMillis() + "_" + originalFilename;
        Path destinationFile = uploadDirectory.resolve(filename);

        try {
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File saved successfully at: " + destinationFile);
            return destinationFile.toString();
        } catch (IOException e) {
            System.err.println("Failed to save file: " + e.getMessage());
            throw new IOException("Failed to save uploaded file", e);
        }
    }


    // Retrieve journal entries by status with optional filtering (dummy implementation - remains unchanged)
    public List<JournalEntry> getJournalEntriesByStatus(String status, String dateFrom, String dateTo, String search) {
        JournalStatus statusEnum = JournalStatus.valueOf(status.toUpperCase());
        return journalEntryRepository.findByStatus(statusEnum);
    }

    public List<JournalEntry> findByStatus(String status) {
        JournalStatus statusEnum = JournalStatus.valueOf(status.toUpperCase());
        return journalEntryRepository.findByStatus(statusEnum);
    }

    // getAllEntriesFiltered method remains unchanged
    public List<JournalEntry> getAllEntriesFiltered(String status, String startDate, String endDate, String search) {
        // ... (implementation remains the same) ...
        JournalStatus journalStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                journalStatus = JournalStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Ignore invalid status
            }
        }

        LocalDate start = (startDate != null && !startDate.isEmpty()) ? LocalDate.parse(startDate) : LocalDate.MIN;
        LocalDate end = (endDate != null && !endDate.isEmpty()) ? LocalDate.parse(endDate) : LocalDate.of(9999, 12, 31);

        List<JournalEntry> entries;
        if (journalStatus != null) {
            entries = journalEntryRepository.findByStatusAndEntryDateBetween(journalStatus, start, end);
        } else {
            entries = journalEntryRepository.findAll().stream()
                        .filter(e -> e.getEntryDate() != null && !e.getEntryDate().isBefore(start) && !e.getEntryDate().isAfter(end))
                        .collect(Collectors.toList());
        }

        if (search != null && !search.trim().isEmpty()) {
            String lowerSearch = search.toLowerCase().replaceAll("\\s+", "");
            entries = entries.stream().filter(e ->
                (e.getDescription() != null && e.getDescription().replaceAll("\\s+", "").toLowerCase().contains(lowerSearch))
                ||
                (e.getLines().stream().anyMatch(line ->
                    line.getAccount() != null &&
                    line.getAccount().getAccountName().replaceAll("\\s+", "").toLowerCase().contains(lowerSearch)
                ))
                ||
                (e.getTotalDebit() != null && e.getTotalDebit().toString().contains(lowerSearch))
                ||
                (e.getTotalCredit() != null && e.getTotalCredit().toString().contains(lowerSearch))
                ||
                (e.getEntryDate() != null && e.getEntryDate().toString().contains(lowerSearch))
                ||
                (String.valueOf(e.getId()).contains(lowerSearch)) // Search by ID
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
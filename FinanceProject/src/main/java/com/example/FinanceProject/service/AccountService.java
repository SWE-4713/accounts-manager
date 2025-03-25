// AccountService.java
package com.example.FinanceProject.service;

import com.example.FinanceProject.entity.Account;
import com.example.FinanceProject.repository.AccountRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccountService {

    @Autowired
    private AccountRepo accountRepo;
    
    @Autowired
    private EventLogService eventLogService; // Inject our event log service

    public Account addAccount(Account account) {
        // Check for duplicate account number and name
        if (accountRepo.findByAccountNumber(account.getAccountNumber()).isPresent()) {
            throw new IllegalArgumentException("Account number already exists.");
        }
        if (accountRepo.findByAccountName(account.getAccountName()).isPresent()) {
            throw new IllegalArgumentException("Account name already exists.");
        }
        
        // Set date/time when account is added
        account.setDateAdded(LocalDateTime.now());
        
        // Ensure monetary values have two decimals
        if (account.getInitialBalance() != null) {
            account.setInitialBalance(account.getInitialBalance().setScale(2, RoundingMode.HALF_UP));
        }
        if (account.getDebit() != null) {
            account.setDebit(account.getDebit().setScale(2, RoundingMode.HALF_UP));
        }
        if (account.getCredit() != null) {
            account.setCredit(account.getCredit().setScale(2, RoundingMode.HALF_UP));
        }
        if (account.getBalance() != null) {
            account.setBalance(account.getBalance().setScale(2, RoundingMode.HALF_UP));
        }
        
        Account saved = accountRepo.save(account);
        // Log event: since this is a new record, beforeImage is null.
        eventLogService.logEvent(null, saved, saved.getUserId(), "ADD");
        return saved;
    }
    
    public List<Account> getAllAccounts() {
        return accountRepo.findAll();
    }
    
    public Account getAccountById(Long id) {
        return accountRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found."));
    }
    
    public Account updateAccount(Account updatedAccount) {
        Account existing = getAccountById(updatedAccount.getId());
        
        // Check if account number/name is changed to one that already exists.
        if (!existing.getAccountNumber().equals(updatedAccount.getAccountNumber())) {
            if (accountRepo.findByAccountNumber(updatedAccount.getAccountNumber()).isPresent()) {
                throw new IllegalArgumentException("Account number already exists.");
            }
        }
        if (!existing.getAccountName().equals(updatedAccount.getAccountName())) {
            if (accountRepo.findByAccountName(updatedAccount.getAccountName()).isPresent()) {
                throw new IllegalArgumentException("Account name already exists.");
            }
        }
        
        // Capture before-image for event log
        Account beforeUpdate = new Account();
        beforeUpdate.setId(existing.getId());
        beforeUpdate.setAccountName(existing.getAccountName());
        beforeUpdate.setAccountNumber(existing.getAccountNumber());
        beforeUpdate.setBalance(existing.getBalance());
        // (Copy other fields as needed)

        // Update fields (other fields such as dateAdded remain unchanged)
        existing.setAccountName(updatedAccount.getAccountName());
        existing.setAccountNumber(updatedAccount.getAccountNumber());
        existing.setAccountDescription(updatedAccount.getAccountDescription());
        existing.setNormalSide(updatedAccount.getNormalSide());
        existing.setAccountCategory(updatedAccount.getAccountCategory());
        existing.setAccountSubcategory(updatedAccount.getAccountSubcategory());
        existing.setInitialBalance(updatedAccount.getInitialBalance().setScale(2, RoundingMode.HALF_UP));
        existing.setDebit(updatedAccount.getDebit().setScale(2, RoundingMode.HALF_UP));
        existing.setCredit(updatedAccount.getCredit().setScale(2, RoundingMode.HALF_UP));
        existing.setBalance(updatedAccount.getBalance().setScale(2, RoundingMode.HALF_UP));
        existing.setUserId(updatedAccount.getUserId());
        existing.setAccountOrder(updatedAccount.getAccountOrder());
        existing.setStatement(updatedAccount.getStatement());
        existing.setComment(updatedAccount.getComment());
        
        Account saved = accountRepo.save(existing);
        // Log event: record before and after images.
        eventLogService.logEvent(beforeUpdate, saved, saved.getUserId(), "MODIFY");
        return saved;
    }
    
    public void deactivateAccount(Long id) {
        Account account = getAccountById(id);
        // 6. Check that balance is zero before deactivation
        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Accounts with balance greater than zero cannot be deactivated.");
        }
        
        // Capture before-image for event log
        Account beforeDeactivation = new Account();
        beforeDeactivation.setId(account.getId());
        beforeDeactivation.setActive(account.isActive());
        // (Copy additional fields if needed)
        
        account.setActive(false);
        Account saved = accountRepo.save(account);
        
        // Log event: record deactivation event
        eventLogService.logEvent(beforeDeactivation, saved, saved.getUserId(), "DEACTIVATE");
    }
    
    // 8 & 12. Search/filter accounts by various tokens (account name, number, category, etc.)
    public List<Account> searchAccounts(String query) {
        // For simplicity, we use a contains search on both account number and name.
        return accountRepo.findByAccountNumberContainingOrAccountNameContaining(query, query);
    }
}

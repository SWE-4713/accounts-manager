package com.example.FinanceProject.service;

import com.example.FinanceProject.Account;
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
    
    public Account addAccount(Account account) {
        // 2. Check for duplicate account number and name
        if (accountRepo.findByAccountNumber(account.getAccountNumber()).isPresent()) {
            throw new IllegalArgumentException("Account number already exists.");
        }
        if (accountRepo.findByAccountName(account.getAccountName()).isPresent()) {
            throw new IllegalArgumentException("Account name already exists.");
        }
        
        // k. Set date/time when account is added
        account.setDateAdded(LocalDateTime.now());
        
        // 3. Ensure monetary values have two decimals (set the scale)
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

        return accountRepo.save(account);
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
        
        // Check if account number/name is changed to an already existing one.
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
        
        // Update fields (other fields such as dateAdded can remain unchanged)
        existing.setAccountName(updatedAccount.getAccountName());
        existing.setAccountNumber(updatedAccount.getAccountNumber());
        existing.setAccountDescription(updatedAccount.getAccountDescription());
        existing.setNormalSide(updatedAccount.getNormalSide());
        existing.setAccountCategory(updatedAccount.getAccountCategory());
        existing.setAccountSubcategory(updatedAccount.getAccountSubcategory());
        existing.setInitialBalance(updatedAccount.getInitialBalance().setScale(2, BigDecimal.ROUND_HALF_UP));
        existing.setDebit(updatedAccount.getDebit().setScale(2, BigDecimal.ROUND_HALF_UP));
        existing.setCredit(updatedAccount.getCredit().setScale(2, BigDecimal.ROUND_HALF_UP));
        existing.setBalance(updatedAccount.getBalance().setScale(2, BigDecimal.ROUND_HALF_UP));
        existing.setUserId(updatedAccount.getUserId());
        existing.setAccountOrder(updatedAccount.getAccountOrder());
        existing.setStatement(updatedAccount.getStatement());
        existing.setComment(updatedAccount.getComment());
        
        return accountRepo.save(existing);
    }
    
    public void deactivateAccount(Long id) {
        Account account = getAccountById(id);
        account.setActive(false);
        accountRepo.save(account);
    }
}

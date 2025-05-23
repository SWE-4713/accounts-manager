// AccountService.java
package com.example.FinanceProject.service;

import com.example.FinanceProject.entity.Account;
import com.example.FinanceProject.repository.AccountRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.FinanceProject.entity.User;
import org.springframework.data.domain.Sort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

@Service
public class AccountService {

    @Autowired
    private AccountRepo accountRepo;
    
    @Autowired
    private EventLogService eventLogService; // Inject our event log service

    @Autowired
    private UserService userService;    

    public Account addAccount(Account account) {
        // Check for duplicate names if needed
        if (accountRepo.findByAccountName(account.getAccountName()).isPresent()) {
            throw new IllegalArgumentException("Account name already exists.");
        }

        // Auto-generate the account number
        String generatedNumber = generateAccountNumber(account);
        account.setAccountNumber(generatedNumber);

        // Set the date/time account was added
        account.setDateAdded(LocalDateTime.now());

        // Set active flag explicitly if needed
        account.setActive(true);

        // Format monetary values to two decimals
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

        // Set the current user (the creator) on the account
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && !auth.getName().equals("anonymousUser")) {
            User currentUser = userService.findUserByUsername(auth.getName());
            account.setUser(currentUser);
        }

        // Save account and log event
        Account saved = accountRepo.save(account);
        eventLogService.logEvent(null, saved, saved.getUserId(), "ADD");
        return saved;
    }

    private String generateAccountNumber(Account account) {
        // Map account category to a first digit
        String category = account.getAccountCategory();
        char firstDigit;
        switch (category.toLowerCase()) {
            case "asset":
                firstDigit = '1';
                break;
            case "liability":
                firstDigit = '2';
                break;
            case "equity":
                firstDigit = '3';
                break;
            case "revenue":
                firstDigit = '4';
                break;
            case "expense":
                firstDigit = '5';
                break;
            default:
                firstDigit = '6';
                break;
        }
        String prefix = String.valueOf(firstDigit);
        Optional<Account> lastOpt = accountRepo.findTopByAccountNumberStartingWithOrderByAccountNumberDesc(prefix);
        long newNumber;
        if (lastOpt.isPresent()) {
            String lastStr = lastOpt.get().getAccountNumber();
            long lastNumber = Long.parseLong(lastStr);
            newNumber = lastNumber + 10;
        } else {
            // e.g., start with 1000000010 for assets (if firstDigit == '1')
            newNumber = Long.parseLong(prefix + "000000010");
        }
        return String.format("%010d", newNumber);
    }
    
    public List<Account> getAllAccounts(Sort sort) {
        return accountRepo.findAll(sort);
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
        beforeUpdate.setAccountName(existing.getAccountName());
        beforeUpdate.setAccountNumber(existing.getAccountNumber());
        beforeUpdate.setAccountDescription(existing.getAccountDescription());
        beforeUpdate.setNormalSide(existing.getNormalSide());
        beforeUpdate.setAccountCategory(existing.getAccountCategory());
        beforeUpdate.setAccountSubcategory(existing.getAccountSubcategory());
        beforeUpdate.setInitialBalance(existing.getInitialBalance());
        beforeUpdate.setDebit(existing.getDebit());
        beforeUpdate.setCredit(existing.getCredit());
        beforeUpdate.setBalance(existing.getBalance());
        beforeUpdate.setUserId(existing.getUserId());
        beforeUpdate.setAccountOrder(existing.getAccountOrder());
        beforeUpdate.setStatement(existing.getStatement());
        beforeUpdate.setComment(existing.getComment());
        // (Copy other fields as needed)

        // Update fields (other fields such as dateAdded remain unchanged)
        existing.setAccountName(updatedAccount.getAccountName());
        existing.setAccountNumber(updatedAccount.getAccountNumber());
        existing.setAccountDescription(updatedAccount.getAccountDescription());
        existing.setNormalSide(updatedAccount.getNormalSide());
        existing.setAccountCategory(updatedAccount.getAccountCategory());
        existing.setAccountSubcategory(updatedAccount.getAccountSubcategory());
        existing.setInitialBalance(updatedAccount.getInitialBalance());
        existing.setDebit(updatedAccount.getDebit());
        existing.setCredit(updatedAccount.getCredit());
        existing.setBalance(updatedAccount.getBalance());
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
        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Accounts with balance greater than zero cannot be deactivated.");
        }
        Account beforeDeactivation = new Account();
        beforeDeactivation.setId(account.getId());
        beforeDeactivation.setActive(account.isActive());
        account.setActive(false);
        Account saved = accountRepo.save(account);
        eventLogService.logEvent(beforeDeactivation, saved, saved.getUser() != null ? saved.getUser().getId() : null, "DEACTIVATE");
    }


    // 8 & 12. Search/filter accounts by various tokens (account name, number, category, etc.)
    public List<Account> searchAccounts(String query, Sort sort) {
        // For simplicity, we use a contains search on both account number and name.
        return accountRepo.findByAccountNumberContainingOrAccountNameContaining(query, query, sort);
    }
    
    public List<Account> filterAccountsByCategory(String category, Sort sort) {
        return accountRepo.findByAccountCategory(category, sort);
    }
    
    public List<Account> searchAndFilterAccounts(String query, String category, Sort sort) {
        // Combine search and filter – here we perform search then filter in memory.
        List<Account> searched = searchAccounts(query, sort);
        return searched.stream()
                       .filter(a -> a.getAccountCategory().equalsIgnoreCase(category))
                       .toList();
    }

    public List<Account> getAccountsForUser(User user) {
        return accountRepo.findByUser(user);
    }

    public List<Account> filterAccountsExtended(String query, String category, String statement,
                                                  String normalSide, String active,
                                                  BigDecimal balanceMin, BigDecimal balanceMax,
                                                  Sort sort) {
        List<Account> accounts = getAllAccounts(sort);
        
        return accounts.stream().filter(a -> {
            boolean matches = true;
            
            if (query != null && !query.trim().isEmpty()) {
                String lowerQuery = query.toLowerCase();
                matches = matches && (a.getAccountName().toLowerCase().contains(lowerQuery)
                                       || a.getAccountNumber().toLowerCase().contains(lowerQuery));
            }
            
            if (category != null && !category.trim().isEmpty()) {
                matches = matches && a.getAccountCategory().equalsIgnoreCase(category);
            }
            
            if (statement != null && !statement.trim().isEmpty()) {
                // Assuming statement field is not null – adjust as needed.
                matches = matches && (a.getStatement() != null && a.getStatement().equalsIgnoreCase(statement));
            }
            
            if (normalSide != null && !normalSide.trim().isEmpty()) {
                matches = matches && (a.getNormalSide() != null && a.getNormalSide().equalsIgnoreCase(normalSide));
            }
            
            if (active != null && !active.trim().isEmpty()) {
                boolean filterActive = active.equalsIgnoreCase("true");
                matches = matches && (a.isActive() == filterActive);
            }
            
            if (balanceMin != null) {
                matches = matches && (a.getBalance().compareTo(balanceMin) >= 0);
            }
            if (balanceMax != null) {
                matches = matches && (a.getBalance().compareTo(balanceMax) <= 0);
            }
            
            return matches;
        }).collect(Collectors.toList());
    }
}
